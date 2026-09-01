package com.mitire.ui.views;

import com.mitire.backend.service.ProjectService;
import com.mitire.backend.web.dto.CreateProjectRequest;
import com.mitire.backend.web.dto.ProjectDto;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "projects", layout = MainLayout.class)
@PageTitle("Projects | Mitire")
@RolesAllowed("ADMIN")
public class ProjectsView extends VerticalLayout {

    private final ProjectService projectService;
    private final Grid<ProjectDto> grid = new Grid<>(ProjectDto.class, false);

    public ProjectsView(ProjectService projectService) {
        this.projectService = projectService;

        add(buildForm());
        add(buildGrid());

        refreshGrid();
    }

    private FormLayout buildForm() {
        TextField code = new TextField("Code");
        TextField name = new TextField("Name");

        Button submit = new Button("Create", e -> {
            if (code.getValue().isBlank() || name.getValue().isBlank()) {
                Notification.show("Code and name are required").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                projectService.create(new CreateProjectRequest(code.getValue(), name.getValue()));
                Notification.show("Project created").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                code.clear();
                name.clear();
                refreshGrid();
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        return new FormLayout(code, name, submit);
    }

    private Grid<ProjectDto> buildGrid() {
        grid.addColumn(ProjectDto::code).setHeader("Code").setSortable(true);
        grid.addColumn(ProjectDto::name).setHeader("Name");
        grid.addColumn(ProjectDto::active).setHeader("Active");
        grid.setWidthFull();
        grid.setHeight("400px");
        return grid;
    }

    private void refreshGrid() {
        grid.setItems(projectService.findAll());
    }
}
