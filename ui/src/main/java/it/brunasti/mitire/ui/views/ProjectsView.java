package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.web.dto.CreateProjectRequest;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.brunasti.mitire.ui.util.Notifications;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "projects", layout = MainLayout.class)
@PageTitle("Projects | MiTiRe")
@RolesAllowed("ADMIN")
public class ProjectsView extends VerticalLayout {

    private final ProjectService projectService;
    private final Grid<ProjectDto> grid = new Grid<>(ProjectDto.class, false);

    public ProjectsView(ProjectService projectService) {
        this.projectService = projectService;

        setSizeFull();

        H2 title = new H2("Projects");

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Projects", buildGrid());
        tabSheet.add("Add Project", buildForm());
        tabSheet.setSizeFull();

        add(title, tabSheet);
        setFlexGrow(1, tabSheet);

        refreshGrid();
    }

    private FormLayout buildForm() {
        TextField code = new TextField("Code");
        TextField name = new TextField("Name");
        DatePicker startDate = new DatePicker("Start date");
        startDate.setWidth("160px");
        DatePicker endDate = new DatePicker("End date");
        endDate.setWidth("160px");

        Button submit = new Button("Create", e -> {
            if (code.getValue().isBlank() || name.getValue().isBlank()) {
                Notifications.showError("Code and name are required");
                return;
            }
            try {
                projectService.create(new CreateProjectRequest(code.getValue(), name.getValue(),
                        startDate.getValue(), endDate.getValue()));
                Notification.show("Project created").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                code.clear();
                name.clear();
                startDate.clear();
                endDate.clear();
                refreshGrid();
            } catch (IllegalArgumentException ex) {
                Notifications.showError(ex.getMessage());
            }
        });

        HorizontalLayout startAndEndDate = new HorizontalLayout(startDate, endDate);

        FormLayout form = new FormLayout(code, name, startAndEndDate, submit);
        form.setMaxWidth("600px");
        return form;
    }

    private Grid<ProjectDto> buildGrid() {
        grid.addColumn(ProjectDto::code).setHeader("Code").setSortable(true);
        grid.addColumn(ProjectDto::name).setHeader("Name").setSortable(true);
        grid.addColumn(ProjectDto::active).setHeader("Active").setSortable(true);
        grid.addColumn(ProjectDto::startDate).setHeader("Start date").setSortable(true);
        grid.addColumn(ProjectDto::endDate).setHeader("End date").setSortable(true);
        grid.setSizeFull();
        grid.getStyle().set("cursor", "pointer");
        grid.addItemClickListener(e -> UI.getCurrent().navigate(ProjectDetailView.class, e.getItem().id()));
        return grid;
    }

    private void refreshGrid() {
        grid.setItems(projectService.findAll());
    }
}
