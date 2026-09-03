package it.brunasti.mitire.ui.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Span;
import it.brunasti.mitire.backend.web.dto.ProjectEntryStatusDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
