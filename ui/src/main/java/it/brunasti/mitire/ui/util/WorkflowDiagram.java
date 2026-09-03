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
 */
public final class WorkflowDiagram {

    private static final int NODE_HEIGHT = 46;
    private static final int ROW_GAP = 18;
    private static final int COLUMN_GAP = 70;
    private static final int MARGIN = 20;
    private static final int CHAR_WIDTH = 8;
    private static final int NODE_PADDING = 28;

    private WorkflowDiagram() {
    }

    public static Component build(List<ProjectEntryStatusDto> statuses,
                                   Function<ProjectEntryStatusDto, List<ProjectEntryStatusDto>> childrenLookup) {
        if (statuses.isEmpty()) {
            return new Span("No statuses defined yet.");
        }

        Map<Long, Integer> level = computeLevels(statuses, childrenLookup);
        Map<Integer, List<ProjectEntryStatusDto>> byLevel = new HashMap<>();
        for (ProjectEntryStatusDto status : statuses) {
            byLevel.computeIfAbsent(level.get(status.id()), k -> new ArrayList<>()).add(status);
        }
        int columnCount = byLevel.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;

        Map<Long, int[]> position = new HashMap<>(); // statusId -> [x, y, width]
        int[] columnX = new int[columnCount];
        int x = MARGIN;
        for (int col = 0; col < columnCount; col++) {
            List<ProjectEntryStatusDto> column = byLevel.getOrDefault(col, List.of()).stream()
                    .sorted(Comparator.comparingInt(ProjectEntryStatusDto::sequence))
                    .toList();
            int columnWidth = column.stream()
                    .mapToInt(s -> Math.max(90, s.name().length() * CHAR_WIDTH + NODE_PADDING))
                    .max().orElse(90);
            columnX[col] = x;
            int y = MARGIN;
            for (ProjectEntryStatusDto status : column) {
                position.put(status.id(), new int[]{x, y, columnWidth});
                y += NODE_HEIGHT + ROW_GAP;
            }
            x += columnWidth + COLUMN_GAP;
        }

        int width = x - COLUMN_GAP + MARGIN;
        int height = byLevel.values().stream().mapToInt(List::size).max().orElse(1) * (NODE_HEIGHT + ROW_GAP) + MARGIN;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
                .append("\" height=\"").append(height)
                .append("\" viewBox=\"0 0 ").append(width).append(' ').append(height).append("\">");
        svg.append("<defs><marker id=\"wf-arrow\" viewBox=\"0 0 10 10\" refX=\"9\" refY=\"5\" "
                + "markerWidth=\"7\" markerHeight=\"7\" orient=\"auto-start-reverse\">"
                + "<path d=\"M0,0 L10,5 L0,10 z\" fill=\"var(--lumo-contrast-60pct)\"/></marker></defs>");

        for (ProjectEntryStatusDto status : statuses) {
            for (ProjectEntryStatusDto child : childrenLookup.apply(status)) {
                int[] from = position.get(status.id());
                int[] to = position.get(child.id());
                if (from == null || to == null) {
                    continue;
                }
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

        for (ProjectEntryStatusDto status : statuses) {
            int[] p = position.get(status.id());
            int nx = p[0];
            int ny = p[1];
            int nw = p[2];
            String stroke = status.startingStatus() ? "var(--lumo-primary-color)" : "var(--lumo-contrast-30pct)";
            String strokeWidth = status.startingStatus() ? "2.5" : "1.5";
            String dash = status.active() ? "" : " stroke-dasharray=\"5,3\"";
            String textColor = status.active() ? "var(--lumo-body-text-color)" : "var(--lumo-secondary-text-color)";
            svg.append("<rect x=\"").append(nx).append("\" y=\"").append(ny)
                    .append("\" width=\"").append(nw).append("\" height=\"").append(NODE_HEIGHT)
                    .append("\" rx=\"8\" fill=\"var(--lumo-base-color)\" stroke=\"").append(stroke)
                    .append("\" stroke-width=\"").append(strokeWidth).append('"').append(dash).append("/>");
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

        String wrapped = "<div style=\"overflow:auto; border: 1px solid var(--lumo-contrast-10pct); "
                + "border-radius: var(--lumo-border-radius-m); padding: 0.5rem;\">" + svg + "</div>";
        return new Html(wrapped);
    }

    private static Map<Long, Integer> computeLevels(List<ProjectEntryStatusDto> statuses,
                                                      Function<ProjectEntryStatusDto, List<ProjectEntryStatusDto>> childrenLookup) {
        Map<Long, Integer> level = new HashMap<>();
        for (ProjectEntryStatusDto status : statuses) {
            level.put(status.id(), 0);
        }
        boolean changed = true;
        int guard = 0;
        while (changed && guard++ <= statuses.size()) {
            changed = false;
            for (ProjectEntryStatusDto status : statuses) {
                int parentLevel = level.get(status.id());
                for (ProjectEntryStatusDto child : childrenLookup.apply(status)) {
                    if (parentLevel + 1 > level.getOrDefault(child.id(), 0)) {
                        level.put(child.id(), parentLevel + 1);
                        changed = true;
                    }
                }
            }
        }
        return level;
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
