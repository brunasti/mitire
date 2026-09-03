package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.service.TimeEntryService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.TimeEntryDto;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.brunasti.mitire.ui.util.Formatters;
import it.brunasti.mitire.ui.util.TimePeriodFilter;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "all-time-entries", layout = MainLayout.class)
@PageTitle("All Time Entries | MiTiRe")
@RolesAllowed("ADMIN")
public class AllTimeEntriesView extends VerticalLayout {

    private final TimeEntryService timeEntryService;

    private final Grid<TimeEntryDto> grid = new Grid<>(TimeEntryDto.class, false);
    private final ComboBox<ProjectDto> projectFilter = new ComboBox<>("Project");
    private final ComboBox<UserDto> userFilter = new ComboBox<>("User");
    private final TimePeriodFilter periodFilter = new TimePeriodFilter();

    public AllTimeEntriesView(TimeEntryService timeEntryService, ProjectService projectService,
                               UserService userService) {
        this.timeEntryService = timeEntryService;

        setSizeFull();

        H2 title = new H2("All Time Entries");

        projectFilter.setItems(projectService.findAll());
        projectFilter.setItemLabelGenerator(p -> p.code() + " - " + p.name());
        projectFilter.setClearButtonVisible(true);
        projectFilter.addValueChangeListener(e -> refreshGrid());

        userFilter.setItems(userService.findAll());
        userFilter.setItemLabelGenerator(UserDto::username);
        userFilter.setClearButtonVisible(true);
        userFilter.addValueChangeListener(e -> refreshGrid());

        periodFilter.addFilterChangeListener(this::refreshGrid);

        HorizontalLayout filters = new HorizontalLayout(projectFilter, userFilter, periodFilter);
        filters.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        grid.addColumn(TimeEntryDto::workDate).setHeader("Date").setSortable(true).setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(TimeEntryDto::username).setHeader("User").setSortable(true);
        grid.addColumn(TimeEntryDto::projectCode).setHeader("Project").setSortable(true).setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(TimeEntryDto::hours).setHeader("Hours").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(TimeEntryDto::description).setHeader("Description");
        grid.addColumn(TimeEntryDto::statusName).setHeader("Status");
        grid.addColumn(e -> Formatters.timestamp(e.createdAt())).setHeader("Created").setSortable(true)
                .setAutoWidth(true).setFlexGrow(0);
        grid.setSizeFull();
        grid.getStyle().set("cursor", "pointer");
        grid.addItemClickListener(e -> UI.getCurrent().navigate(TimeEntryDetailView.class, e.getItem().id()));

        add(title, filters, grid);
        setFlexGrow(1, grid);

        refreshGrid();
    }

    private void refreshGrid() {
        Long projectId = projectFilter.getValue() != null ? projectFilter.getValue().id() : null;
        Long userId = userFilter.getValue() != null ? userFilter.getValue().id() : null;
        TimePeriodFilter.DateRange range = periodFilter.getRange();
        grid.setItems(timeEntryService.search(userId, projectId, range.from(), range.to()));
    }
}
