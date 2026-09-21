package it.brunasti.mitire.ui.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Span;
import it.brunasti.mitire.backend.web.dto.ProjectEntryStatusDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowDiagramTest {

    private static ProjectEntryStatusDto status(long id, String name, int sequence, boolean starting) {
        return new ProjectEntryStatusDto(id, 1L, name, sequence, true, starting, null);
    }

    @Test
    void buildReturnsPlaceholderWhenNoStatusesExist() {
        Component result = WorkflowDiagram.build(List.of(), s -> List.of());

        assertThat(result).isInstanceOf(Span.class);
        assertThat(((Span) result).getText()).isEqualTo("No statuses defined yet.");
    }

    @Test
    void buildRendersSvgWithEveryStatusNameAndAnArrowPerTransition() {
        ProjectEntryStatusDto submitted = status(1, "SUBMITTED", 1, true);
        ProjectEntryStatusDto approved = status(2, "APPROVED", 2, false);
        ProjectEntryStatusDto rejected = status(3, "REJECTED", 3, false);

        Map<Long, List<ProjectEntryStatusDto>> edges = Map.of(
                1L, List.of(approved, rejected),
                2L, List.of(),
                3L, List.of()
        );

        Component result = WorkflowDiagram.build(List.of(submitted, approved, rejected),
                s -> edges.get(s.id()));

        assertThat(result).isInstanceOf(Html.class);
        String html = ((Html) result).getInnerHtml();
        assertThat(html).contains("<svg");
        assertThat(html).contains("SUBMITTED").contains("APPROVED").contains("REJECTED");
        assertThat(html.split("marker-end", -1).length - 1).isEqualTo(2);
        // orient="auto" (not "auto-start-reverse") so the arrowhead points along the path's
        // actual end tangent, i.e. into the child status, not back toward the parent.
        assertThat(html).contains("orient=\"auto\"").doesNotContain("auto-start-reverse");
        assertThat(html).contains("style=\"flex-shrink:0;\"");
        // Centers the diagram when the wrapping tab has more space than the diagram needs;
        // this style lives on the wrapper <div>, i.e. the Html component's own root element,
        // so it's on the element itself rather than inside getInnerHtml().
        String wrapperStyle = result.getElement().getAttribute("style");
        assertThat(wrapperStyle).contains("display:flex").contains("justify-content:center");
    }

    @Test
    void buildTerminatesAndRendersAllNodesWhenTheWorkflowGraphHasACycle() {
        // Mirrors a real seeded workflow: SUBMITTED -> REVIEW -> APPROVED/REJECTED,
        // and REJECTED -> SUBMITTED again (a cycle), which the layering pass must
        // not loop forever on.
        ProjectEntryStatusDto submitted = status(1, "SUBMITTED", 1, true);
        ProjectEntryStatusDto review = status(2, "REVIEW", 2, false);
        ProjectEntryStatusDto approved = status(3, "APPROVED", 3, false);
        ProjectEntryStatusDto rejected = status(4, "REJECTED", 4, false);

        Map<Long, List<ProjectEntryStatusDto>> edges = Map.of(
                1L, List.of(review),
                2L, List.of(approved, rejected),
                3L, List.of(),
                4L, List.of(submitted)
        );

        Component result = WorkflowDiagram.build(List.of(submitted, review, approved, rejected),
                s -> edges.get(s.id()));

        String html = ((Html) result).getInnerHtml();
        assertThat(html).contains("SUBMITTED").contains("REVIEW").contains("APPROVED").contains("REJECTED");

        // The cycle (REJECTED -> SUBMITTED) must not drag the starting status
        // rightward: it should still be the leftmost box in the diagram.
        int submittedX = boxX(html, "SUBMITTED");
        assertThat(submittedX).isLessThan(boxX(html, "REVIEW"));
        assertThat(submittedX).isLessThan(boxX(html, "APPROVED"));
        assertThat(submittedX).isLessThan(boxX(html, "REJECTED"));
    }

    private static int boxX(String svg, String statusName) {
        Pattern pattern = Pattern.compile("<rect x=\"(\\d+)\"[^>]*/>\\s*<text[^>]*>" + statusName + "</text>");
        Matcher matcher = pattern.matcher(svg);
        assertThat(matcher.find()).as("rect for " + statusName).isTrue();
        return Integer.parseInt(matcher.group(1));
    }

    @Test
    void buildKeepsColumnsContiguousWhenACycleDoesNotTouchTheStartingStatus() {
        // Mirrors a real seeded workflow: SUBMITTED -> REVIEW -> APPROVED/REJECTED ->
        // X1 -> REVIEW again, i.e. a cycle entirely among the non-starting statuses.
        // Naive repeated relaxation keeps pushing REVIEW/APPROVED/REJECTED/X1 further
        // right on every lap of that cycle, leaving many empty columns between them
        // and SUBMITTED - which renders as SUBMITTED sitting far away, detached from
        // the rest of the diagram, even though it's still technically "leftmost".
        ProjectEntryStatusDto submitted = status(1, "SUBMITTED", 1, true);
        ProjectEntryStatusDto review = status(2, "REVIEW", 2, false);
        ProjectEntryStatusDto approved = status(3, "APPROVED", 3, false);
        ProjectEntryStatusDto rejected = status(4, "REJECTED", 4, false);
        ProjectEntryStatusDto x1 = status(5, "X1", 5, false);

        Map<Long, List<ProjectEntryStatusDto>> edges = Map.of(
                1L, List.of(review),
                2L, List.of(approved, rejected),
                3L, List.of(x1),
                4L, List.of(x1),
                5L, List.of(review)
        );

        Component result = WorkflowDiagram.build(List.of(submitted, review, approved, rejected, x1),
                s -> edges.get(s.id()));

        String html = ((Html) result).getInnerHtml();
        int submittedX = boxX(html, "SUBMITTED");
        int reviewX = boxX(html, "REVIEW");
        int approvedX = boxX(html, "APPROVED");
        int rejectedX = boxX(html, "REJECTED");
        int x1X = boxX(html, "X1");

        assertThat(submittedX).isLessThan(reviewX);
        assertThat(reviewX).isLessThan(approvedX);
        assertThat(reviewX).isLessThan(rejectedX);
        assertThat(approvedX).isLessThan(x1X);
        assertThat(rejectedX).isLessThan(x1X);
        // No box should be stranded far from its neighbor: consecutive columns differ
        // by one box width plus the fixed gap, never by several empty columns' worth.
        assertThat(reviewX - submittedX).isLessThan(400);
    }

    @Test
    void buildRoutesEveryTransitionAsADistinctOrthogonalPath() {
        // Mirrors a real seeded workflow that renders as a single-file chain (one status
        // per column): SUBMITTED -> APPROVED -> REJECTED -> OTHER -> VERYLONGNAME, plus a
        // skip transition (SUBMITTED -> REJECTED) and a cycle back to the start
        // (OTHER -> SUBMITTED). Without the diagonal stagger every one of these six
        // transitions is drawn on the exact same horizontal line - so most of them are
        // invisible under the others. Each arrow's path must be its own distinct line.
        ProjectEntryStatusDto submitted = status(1, "SUBMITTED", 1, true);
        ProjectEntryStatusDto approved = status(2, "APPROVED", 2, false);
        ProjectEntryStatusDto rejected = status(3, "REJECTED", 3, false);
        ProjectEntryStatusDto other = status(4, "OTHER", 4, false);
        ProjectEntryStatusDto veryLongName = status(5, "VERYLONGNAME", 5, false);

        Map<Long, List<ProjectEntryStatusDto>> edges = Map.of(
                1L, List.of(approved, rejected),
                2L, List.of(rejected),
                3L, List.of(other),
                4L, List.of(submitted, veryLongName),
                5L, List.of()
        );

        Component result = WorkflowDiagram.build(
                List.of(submitted, approved, rejected, other, veryLongName), s -> edges.get(s.id()));

        String html = ((Html) result).getInnerHtml();
        List<String> paths = extractPaths(html);
        assertThat(paths).hasSize(6);
        // The 5 forward transitions are each an orthogonal (horizontal/vertical/
        // horizontal) elbow; the 1 back transition (OTHER -> SUBMITTED, a cycle) drops
        // from the bottom instead (vertical/horizontal/vertical) - see the dedicated
        // test below. Either way, no two transitions are drawn as the literal same line.
        long forwardShaped = paths.stream().filter(p -> p.matches("M\\d+,\\d+ H\\d+ V\\d+ H\\d+")).count();
        long backShaped = paths.stream().filter(p -> p.matches("M\\d+,\\d+ V\\d+ H\\d+ V\\d+")).count();
        assertThat(forwardShaped).isEqualTo(5);
        assertThat(backShaped).isEqualTo(1);
        assertThat(paths).doesNotHaveDuplicates();
    }

    @Test
    void buildRoutesBackEdgesFromTheBottomOfTheParentIntoTheBottomOfTheChild() {
        // Same cyclic workflow as buildTerminatesAndRendersAllNodesWhenTheWorkflowGraphHasACycle:
        // SUBMITTED -> REVIEW -> APPROVED/REJECTED, and REJECTED -> SUBMITTED (a cycle).
        ProjectEntryStatusDto submitted = status(1, "SUBMITTED", 1, true);
        ProjectEntryStatusDto review = status(2, "REVIEW", 2, false);
        ProjectEntryStatusDto approved = status(3, "APPROVED", 3, false);
        ProjectEntryStatusDto rejected = status(4, "REJECTED", 4, false);

        Map<Long, List<ProjectEntryStatusDto>> edges = Map.of(
                1L, List.of(review),
                2L, List.of(approved, rejected),
                3L, List.of(),
                4L, List.of(submitted)
        );

        Component result = WorkflowDiagram.build(List.of(submitted, review, approved, rejected),
                s -> edges.get(s.id()));

        String html = ((Html) result).getInnerHtml();
        List<String> paths = extractPaths(html);
        List<String> backEdgePaths = paths.stream().filter(p -> p.matches("M\\d+,\\d+ V\\d+ H\\d+ V\\d+")).toList();
        assertThat(backEdgePaths).hasSize(1);

        Matcher startMatcher = Pattern.compile("^M(\\d+),(\\d+)").matcher(backEdgePaths.get(0));
        assertThat(startMatcher.find()).isTrue();
        int startY = Integer.parseInt(startMatcher.group(2));
        // REJECTED (the parent of the cycle edge) - the line must start exactly at the
        // bottom of its box, not its side or middle.
        assertThat(startY).isEqualTo(boxY(html, "REJECTED") + 46);
    }

    private static List<String> extractPaths(String svg) {
        Matcher pathMatcher = Pattern.compile("<path d=\"(M[^\"]+)\" fill=\"none\"").matcher(svg);
        List<String> paths = new ArrayList<>();
        while (pathMatcher.find()) {
            paths.add(pathMatcher.group(1));
        }
        return paths;
    }

    private static int boxY(String svg, String statusName) {
        Pattern pattern = Pattern.compile("<rect x=\"\\d+\" y=\"(\\d+)\"[^>]*/>\\s*<text[^>]*>" + statusName + "</text>");
        Matcher matcher = pattern.matcher(svg);
        assertThat(matcher.find()).as("rect for " + statusName).isTrue();
        return Integer.parseInt(matcher.group(1));
    }

    @Test
    void buildMarksTheStartingStatusAndDashesInactiveOnes() {
        ProjectEntryStatusDto starting = status(1, "SUBMITTED", 1, true);
        ProjectEntryStatusDto inactive = new ProjectEntryStatusDto(2L, 1L, "ARCHIVED", 2, false, false, null);

        Component result = WorkflowDiagram.build(List.of(starting, inactive), s -> List.of());

        String html = ((Html) result).getInnerHtml();
        assertThat(html).contains("var(--lumo-primary-color)");
        assertThat(html).contains("stroke-dasharray");
    }
}
