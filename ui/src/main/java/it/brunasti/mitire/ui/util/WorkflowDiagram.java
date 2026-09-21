package it.brunasti.mitire.ui.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Span;
import it.brunasti.mitire.backend.web.dto.ProjectEntryStatusDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Renders a project's approval workflow (its statuses and the direct-transition edges
 * between them) as an inline SVG flowchart: one column per "distance from a root status"
 * (the longest-path layering used for classic DAG/flowchart drawing), left to right, with
 * an arrow per allowed transition. Colors are all Lumo CSS custom properties so the
 * diagram matches the current theme automatically.
 *
 * <p>The whole thing is built in four passes over {@code statuses}, all inside
 * {@link #build}: (1) assign each status a "column" via {@link #computeLevels}, (2)
 * turn columns into concrete pixel positions, (3) draw one arrow per status/child pair,
 * (4) draw the status boxes on top of the arrows so the arrow ends tuck neatly under them.
 */
public final class WorkflowDiagram {

    // Pixel dimensions of one status box, and the gaps between boxes. Tune these to make
    // the diagram denser/sparser; everything else (SVG size, centering) derives from them.
    private static final int NODE_HEIGHT = 46;
    private static final int ROW_GAP = 18;
    private static final int COLUMN_GAP = 70;
    private static final int MARGIN = 8;
    // Each column is also shifted down by one extra step per column, staggering the
    // whole layout diagonally. Without this, workflows that are mostly a single chain
    // (one status per column) put every box on the same row, so distinct transitions -
    // especially a transition that skips a column, or a cycle edge going back several
    // columns - end up drawn as the exact same horizontal line and become
    // indistinguishable. Must clear more than half a box's height (NODE_HEIGHT / 2) so
    // an edge's long horizontal run at one column's row is guaranteed to pass above or
    // below any box in a column between it and where it bends; see the comment on the
    // arrow-drawing pass below for why that guarantee holds.
    private static final int DIAGONAL_STEP = 32;
    // Back edges (real cycles in the workflow, e.g. a "resubmit" transition) are routed
    // through a shared lane below the deepest box (see the pass 3 comment). GAP is how
    // far below the deepest box the first lane sits; STEP is the spacing between lanes
    // when there's more than one back edge, so they don't run on top of each other.
    private static final int BACK_EDGE_LANE_GAP = 20;
    private static final int BACK_EDGE_LANE_STEP = 16;
    // A status box is sized to fit its name: roughly CHAR_WIDTH px per character, plus
    // NODE_PADDING for the left/right inset, with a floor so short names still look boxy.
    private static final int CHAR_WIDTH = 8;
    private static final int NODE_PADDING = 28;

    private WorkflowDiagram() {
    }

    public static Component build(List<ProjectEntryStatusDto> statuses,
                                   Function<ProjectEntryStatusDto, List<ProjectEntryStatusDto>> childrenLookup) {
        System.err.println("WorkflowDiagram");
        if (statuses.isEmpty()) {
            return new Span("No statuses defined yet.");
        }

        System.err.println("WorkflowDiagram - 1");
        // Pass 1: which column (0, 1, 2, ...) does each status belong to? See
        // computeLevels() for how "column" is defined for a graph that isn't
        // necessarily a clean top-to-bottom tree.
        Map<Long, Integer> level = computeLevels(statuses, childrenLookup);

        int min = 99;
        for (Long l : level.keySet()) {
            System.err.println("computeLevels - 99 - l ["+l+"] ["+level.get(l)+"]");
            if (level.get(l) < min) {
                min = level.get(l);
            }
        }
//        min = min - 1;
        if (min < 0) {
            min = 0;
        }
        System.err.println("WorkflowDiagram - 1.1 min = "+ min);

        Map<Integer, List<ProjectEntryStatusDto>> byLevel = new HashMap<>();
        for (ProjectEntryStatusDto status : statuses) {
            System.err.println("WorkflowDiagram - 1.2 ["+status+"]");
            byLevel.computeIfAbsent(level.get(status.id()), k -> new ArrayList<>()).add(status);
        }
        int columnCount = byLevel.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        System.err.println("WorkflowDiagram - 1 ["+columnCount+"]");

        System.err.println("WorkflowDiagram - 2");
        // Pass 2: lay the columns out left to right, and the statuses within a column
        // top to bottom (ordered by the admin's own "sequence"/Order field, so the
        // diagram's vertical order matches the Workflow tab's grid). Each column is as
        // wide as its widest status box, so narrower columns don't waste horizontal
        // space; `position` remembers where every status ended up so pass 3 and pass 4
        // can both look it up by id. Each column also starts DIAGONAL_STEP px lower than
        // the previous one (see the field comment) so the whole diagram staggers
        // diagonally instead of every column sharing the same rows.
        Map<Long, int[]> position = new HashMap<>(); // statusId -> [x, y, boxWidth, col]
        int x = MARGIN;
        for (int col = min; col < columnCount; col++) {
            System.err.println("WorkflowDiagram - 2.1 ["+col+"]");
            List<ProjectEntryStatusDto> column = byLevel.getOrDefault(col, List.of()).stream()
                    .sorted(Comparator.comparingInt(ProjectEntryStatusDto::sequence))
                    .toList();
            int columnWidth = column.stream()
                    .mapToInt(s -> Math.max(90, s.name().length() * CHAR_WIDTH + NODE_PADDING))
                    .max().orElse(90);
            int y = MARGIN + col * DIAGONAL_STEP;
            for (ProjectEntryStatusDto status : column) {
                System.err.println("WorkflowDiagram - 2.2 - ["+status.name()+"]");
                position.put(status.id(), new int[]{x, y, columnWidth, col});
                y += NODE_HEIGHT + ROW_GAP;
            }
            x += columnWidth + COLUMN_GAP;
        }

        System.err.println("WorkflowDiagram - 3");
        // Split transitions into "forward" (child column > parent column) and "back" (a
        // real cycle in the workflow, e.g. a "resubmit" transition) so pass 3 below can
        // route the two differently.
        List<int[][]> forwardEdges = new ArrayList<>();
        List<int[][]> backEdges = new ArrayList<>();
        for (ProjectEntryStatusDto status : statuses) {
            for (ProjectEntryStatusDto child : childrenLookup.apply(status)) {
                int[] from = position.get(status.id());
                int[] to = position.get(child.id());
                if (from == null || to == null) {
                    continue;
                }
                (to[3] > from[3] ? forwardEdges : backEdges).add(new int[][]{from, to});
            }
        }

        // The SVG is sized exactly to its content (x already sits one COLUMN_GAP past
        // the last column, hence subtracting it back out) plus a fixed MARGIN on every
        // side, so there's no dead space baked into the image itself for the wrapping
        // <div> (see the bottom of this method) to have to trim away. Height is driven
        // off the actual bottom-most box rather than a uniform row count, since the
        // diagonal stagger above means later columns sit lower even when they don't
        // have more rows than earlier ones - and, if there are any back edges, extended
        // further to fit their shared lane(s) below that.
        int width = x - COLUMN_GAP + MARGIN;
        int contentBottom = position.values().stream().mapToInt(p -> p[1] + NODE_HEIGHT).max().orElse(NODE_HEIGHT);
        int backEdgeLaneBottom = backEdges.isEmpty() ? contentBottom
                : contentBottom + BACK_EDGE_LANE_GAP + (backEdges.size() - 1) * BACK_EDGE_LANE_STEP;
        int height = Math.max(contentBottom, backEdgeLaneBottom) + MARGIN;

        System.err.println("WorkflowDiagram - 3 - width " + width);
        System.err.println("WorkflowDiagram - 3 - height " + height);

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
                .append("\" height=\"").append(height)
                .append("\" viewBox=\"0 0 ").append(width).append(' ').append(height)
                .append("\" style=\"flex-shrink:0;\">");

        System.err.println("WorkflowDiagram - 4");
        // Arrowhead used at the end of every transition line below. orient="auto" points
        // it along the path's own end tangent (i.e. into the child box) rather than a
        // fixed direction, so it's correct regardless of which way an edge happens to run.
        svg.append("<defs><marker id=\"wf-arrow\" viewBox=\"0 0 10 10\" refX=\"9\" refY=\"5\" "
                + "markerWidth=\"7\" markerHeight=\"7\" orient=\"auto\">"
                + "<path d=\"M0,0 L10,5 L0,10 z\" fill=\"var(--lumo-contrast-60pct)\"/></marker></defs>");

        System.err.println("WorkflowDiagram - 5");
        // Pass 3a: forward transitions, drawn before the boxes so the boxes (opaque
        // fill) paint over the line's start/end, leaving a clean edge. Each is a
        // right-angle "elbow": it leaves the parent's right edge, runs horizontally at
        // the PARENT's row to a bend point just before the child's column, then
        // drops/rises vertically and enters the child's left edge horizontally.
        // Routing the long horizontal run at the PARENT's row, not the child's, is what
        // keeps it from cutting through any box in a column it passes over: an earlier
        // column always sits higher (see DIAGONAL_STEP), so the parent's row clears
        // every column between it and the bend. The vertical segment itself is always
        // in a column GAP, which never has a box in it.
        for (int[][] edge : forwardEdges) {
            int[] from = edge[0];
            int[] to = edge[1];
            int y1 = from[1] + NODE_HEIGHT / 2;
            int y2 = to[1] + NODE_HEIGHT / 2;
            int x1 = from[0] + from[2];
            int x2 = to[0];
            int bendX = x2 - COLUMN_GAP / 2;
            svg.append("<path d=\"M").append(x1).append(',').append(y1)
                    .append(" H").append(bendX).append(" V").append(y2).append(" H").append(x2)
                    .append("\" fill=\"none\" stroke=\"var(--lumo-contrast-60pct)\" stroke-width=\"1.5\" "
                            + "marker-end=\"url(#wf-arrow)\"/>");
        }

        // Pass 3b: back transitions (real cycles, e.g. a "resubmit"). These read very
        // differently from a forward transition on purpose: instead of leaving from a
        // side, the line drops straight down out of the BOTTOM of the parent box, into
        // a lane below every box in the diagram (each back edge gets its own lane,
        // BACK_EDGE_LANE_STEP apart, so several don't run on top of each other), runs
        // left or right there, then rises back up into the BOTTOM of the child box.
        // Since the lane is below the deepest box, its horizontal run can never cut
        // through anything, so this needs no per-column bend point at all.
        for (int i = 0; i < backEdges.size(); i++) {
            int[] from = backEdges.get(i)[0];
            int[] to = backEdges.get(i)[1];
            int x1 = from[0] + from[2] / 2;
            int y1 = from[1] + NODE_HEIGHT;
            int laneY = contentBottom + BACK_EDGE_LANE_GAP + i * BACK_EDGE_LANE_STEP;
            int x2 = to[0] + to[2] / 2;
            int y2 = to[1] + NODE_HEIGHT;
            svg.append("<path d=\"M").append(x1).append(',').append(y1)
                    .append(" V").append(laneY).append(" H").append(x2).append(" V").append(y2)
                    .append("\" fill=\"none\" stroke=\"var(--lumo-contrast-60pct)\" stroke-width=\"1.5\" "
                            + "marker-end=\"url(#wf-arrow)\"/>");
        }

        System.err.println("WorkflowDiagram - 6");
        // Pass 4: the status boxes themselves, on top of the arrows. The starting
        // status gets a thicker primary-color border plus a star; inactive statuses get
        // a dashed border and muted text, matching how they're shown in the grid.
        for (ProjectEntryStatusDto status : statuses) {
            System.err.println("WorkflowDiagram - 6 - status : "+ status.name());
            int[] p = position.get(status.id());
            System.err.println("WorkflowDiagram - 6 - p : " + p[0] + " " + p[1] + " " + p[2] + " " + p[3]);
            int nx = p[0];
            int ny = p[1];
            int nw = p[2];
            int col = p[3];
            String stroke = status.startingStatus() ? "var(--lumo-primary-color)" : "var(--lumo-contrast-30pct)";
            String strokeWidth = status.startingStatus() ? "2.5" : "1.5";
            String dash = status.active() ? "" : " stroke-dasharray=\"5,3\"";
            String textColor = status.active() ? "var(--lumo-body-text-color)" : "var(--lumo-secondary-text-color)";
            svg.append("<rect x=\"").append(nx).append("\" y=\"").append(ny)
                    .append("\" width=\"").append(nw).append("\" height=\"").append(NODE_HEIGHT)
                    .append("\" rx=\"8\" fill=\"var(--lumo-base-color)\" stroke=\"").append(stroke)
                    .append("\" stroke-width=\"").append(strokeWidth).append('"').append(dash).append("/>");

            System.err.println("WorkflowDiagram - ["+status.name()+"] ["+col+"]");
            svg.append("<text x=\"").append(nx + nw / 2).append("\" y=\"").append(ny + NODE_HEIGHT / 2 + 5)
                    .append("\" text-anchor=\"middle\" font-family=\"var(--lumo-font-family)\" font-size=\"13\" "
                            + "font-weight=\"600\" fill=\"").append(textColor).append("\">")
                    .append(escapeXml(status.name())).append("</text>");

            if (status.startingStatus()) {
                svg.append("<text x=\"").append(nx + 6).append("\" y=\"").append(ny + 14)
                        .append("\" font-size=\"12\" fill=\"var(--lumo-primary-color)\">&#9733;</text>");
            }
        }

        svg.append("</svg>");

        System.err.println("WorkflowDiagram - 7");
        // The <svg> is exactly as big as its content (see width/height above), but the
        // tab it lives in usually isn't - so this wrapper centers the SVG within
        // whatever space is actually available, and scrolls (rather than shrinking the
        // SVG, hence flex-shrink:0 on it above) if the diagram is ever bigger than that
        // space instead.
        String wrapped = "<div style=\"box-sizing:border-box; width:100%; height:100%; overflow:auto; "
                + "display:flex; align-items:center; justify-content:center; "
                + "border: 1px solid var(--lumo-contrast-10pct); "
                + "border-radius: var(--lumo-border-radius-m); padding: 0.25rem;\">" + svg + "</div>";

        System.err.println("WorkflowDiagram - 8");
        return new Html(wrapped);
    }

    /**
     * Assigns each status a "column" (0-based) equal to the length of the <em>longest</em>
     * path of transitions that reaches it from any status with no incoming transition -
     * the standard way to turn a directed graph into left-to-right layers for drawing.
     * A status with no parents starts at column 0; each transition can only push its
     * target at least one column further right than its source.
     *
     * <p>Levels are computed by a DFS (starting from the workflow's starting status, so
     * it anchors column 0) that also does a topological sort: any transition that points
     * back to a status still on the current DFS path (an ancestor) is a genuine cycle in
     * the workflow - e.g. a "resubmit" transition - and is recorded as a back edge rather
     * than followed. Levels are then relaxed once, in topological order, over everything
     * <em>except</em> those back edges. Two things fall out of doing it this way instead
     * of naive repeated relaxation: it terminates in a single pass regardless of how the
     * graph's cycles are shaped (nothing can push a level over and over), and it never
     * leaves gaps in the column numbering - every level from 0 up to the maximum has at
     * least one status in it - because reaching column k this way always means passing
     * through a status at every column below it first.
     */
    private static Map<Long, Integer> computeLevels(List<ProjectEntryStatusDto> statuses,
                                                      Function<ProjectEntryStatusDto, List<ProjectEntryStatusDto>> childrenLookup) {
        Map<Long, Integer> level = new HashMap<>();
        for (ProjectEntryStatusDto status : statuses) {
            level.put(status.id(), 0);
        }

        List<ProjectEntryStatusDto> dfsRoots = new ArrayList<>();
        statuses.stream().filter(ProjectEntryStatusDto::startingStatus).findFirst().ifPresent(dfsRoots::add);
        for (ProjectEntryStatusDto status : statuses) {
            if (!dfsRoots.contains(status)) {
                dfsRoots.add(status);
            }
        }

        Set<Long> visiting = new HashSet<>();
        Set<Long> finished = new HashSet<>();
        List<Long> postOrder = new ArrayList<>();
        Map<Long, List<Long>> forwardChildren = new HashMap<>();
        for (ProjectEntryStatusDto root : dfsRoots) {
            if (!finished.contains(root.id())) {
                dfsVisit(root, childrenLookup, visiting, finished, postOrder, forwardChildren);
            }
        }

        // postOrder lists every status in DFS finish order; reversing it gives a valid
        // topological order of the forward-edge-only graph, so each status's level is
        // final by the time it's used as a parent below.
        for (int i = postOrder.size() - 1; i >= 0; i--) {
            Long id = postOrder.get(i);
            int parentLevel = level.get(id);
            for (Long childId : forwardChildren.getOrDefault(id, List.of())) {
                level.put(childId, Math.max(level.get(childId), parentLevel + 1));
            }
        }

        return level;
    }

    private static void dfsVisit(ProjectEntryStatusDto status,
                                  Function<ProjectEntryStatusDto, List<ProjectEntryStatusDto>> childrenLookup,
                                  Set<Long> visiting, Set<Long> finished,
                                  List<Long> postOrder, Map<Long, List<Long>> forwardChildren) {
        visiting.add(status.id());
        for (ProjectEntryStatusDto child : childrenLookup.apply(status)) {
            if (visiting.contains(child.id())) {
                continue; // back edge: closes a cycle, excluded from layering
            }
            forwardChildren.computeIfAbsent(status.id(), k -> new ArrayList<>()).add(child.id());
            if (!finished.contains(child.id())) {
                dfsVisit(child, childrenLookup, visiting, finished, postOrder, forwardChildren);
            }
        }
        visiting.remove(status.id());
        finished.add(status.id());
        postOrder.add(status.id());
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
