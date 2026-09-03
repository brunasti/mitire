package it.brunasti.mitire.ui.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Span;
import it.brunasti.mitire.backend.web.dto.ProjectEntryStatusDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        // can both look it up by id.
        Map<Long, int[]> position = new HashMap<>(); // statusId -> [x, y, boxWidth]
        int x = MARGIN;
        for (int col = min; col < columnCount; col++) {
            System.err.println("WorkflowDiagram - 2.1 ["+col+"]");
            List<ProjectEntryStatusDto> column = byLevel.getOrDefault(col, List.of()).stream()
                    .sorted(Comparator.comparingInt(ProjectEntryStatusDto::sequence))
                    .toList();
            int columnWidth = column.stream()
                    .mapToInt(s -> Math.max(90, s.name().length() * CHAR_WIDTH + NODE_PADDING))
                    .max().orElse(90);
            int y = MARGIN;
            for (ProjectEntryStatusDto status : column) {
                System.err.println("WorkflowDiagram - 2.2 - ["+status.name()+"]");
                position.put(status.id(), new int[]{x, y, columnWidth, col});
                y += NODE_HEIGHT + ROW_GAP;
            }
            x += columnWidth + COLUMN_GAP;
        }

        System.err.println("WorkflowDiagram - 3");
        // The SVG is sized exactly to its content (x already sits one COLUMN_GAP past
        // the last column, hence subtracting it back out) plus a fixed MARGIN on every
        // side, so there's no dead space baked into the image itself for the wrapping
        // <div> (see the bottom of this method) to have to trim away.
        int width = x - COLUMN_GAP + MARGIN;
        int maxRows = byLevel.values().stream().mapToInt(List::size).max().orElse(1);
        int height = maxRows * NODE_HEIGHT + (maxRows - 1) * ROW_GAP + 2 * MARGIN;

        System.err.println("WorkflowDiagram - 3 - width " + width);
        System.err.println("WorkflowDiagram - 3 - maxRows " + maxRows);
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
        // Pass 3: one curved arrow per allowed transition, drawn before the boxes so the
        // boxes (opaque fill) paint over the line's start/end, leaving a clean edge.
        for (ProjectEntryStatusDto status : statuses) {
            for (ProjectEntryStatusDto child : childrenLookup.apply(status)) {
                int[] from = position.get(status.id());
                int[] to = position.get(child.id());
                if (from == null || to == null) {
                    continue;
                }
                // Start at the middle of the parent's right edge, end at the middle of
                // the child's left edge, with a cubic bezier whose control points sit
                // halfway between them (at each end's own height) - the standard
                // "flowchart S-curve" shape, which also degrades gracefully into a
                // straight line when the two boxes are at the same height.
                int x1 = from[0] + from[2];
                int y1 = from[1] + NODE_HEIGHT / 2;
                int x2 = to[0];
                int y2 = to[1] + NODE_HEIGHT / 2;
                int midX = (x1 + x2) / 2;
                svg.append("<path d=\"M").append(x1).append(',').append(y1)
                        .append(" C").append(midX).append(',').append(y1)
                        .append(' ').append(midX).append(',').append(y2)
                        .append(' ').append(x2).append(',').append(y2)
                        .append("\" fill=\"none\" stroke=\"var(--lumo-contrast-60pct)\" stroke-width=\"1.5\" "
                                + "marker-end=\"url(#wf-arrow)\"/>");
            }
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
     * <p>This is computed by repeated relaxation (Bellman-Ford style, since a workflow's
     * dependency graph is small and admins can, in principle, create an invalid one with
     * a cycle - see {@code ProjectEntryStatusService}, which does no cycle detection):
     * each round walks every status and pushes each of its children to at least
     * {@code parentLevel + 1}, repeating until a round makes no more changes.
     *
     * <p>In an acyclic graph with n statuses, the longest possible path has at most
     * n-1 transitions, so levels are fully settled after at most n-1 rounds - one more
     * round could only ever find something new if a cycle keeps pushing a level higher
     * forever. That's exactly the bound used below: it's the fewest rounds that still
     * guarantees a correct, fully-converged layout for any real (acyclic) workflow,
     * while also being what stops the loop promptly if the graph does contain a cycle.
     */
    private static Map<Long, Integer> computeLevels(List<ProjectEntryStatusDto> statuses,
                                                      Function<ProjectEntryStatusDto, List<ProjectEntryStatusDto>> childrenLookup) {
        System.err.println("computeLevels - 1");
        Map<Long, Integer> level = new HashMap<>();
        for (ProjectEntryStatusDto status : statuses) {
            level.put(status.id(), 0);
        }
        int maxRounds = statuses.size() - 1;
        for (int round = 0; round < maxRounds; round++) {
            boolean changed = false;
            for (ProjectEntryStatusDto status : statuses) {
                int parentLevel = level.get(status.id());
                for (ProjectEntryStatusDto child : childrenLookup.apply(status)) {
                    if (parentLevel + 1 > level.getOrDefault(child.id(), 0)) {
                        level.put(child.id(), parentLevel + 1);
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }

        System.err.println("computeLevels - 99 ["+level+"]");
        for (Long l : level.keySet()) {
            System.err.println("computeLevels - 99 - l ["+l+"] ["+level.get(l)+"]");
        }
        return level;
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
