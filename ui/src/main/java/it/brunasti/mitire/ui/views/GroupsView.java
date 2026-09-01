package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.service.GroupService;
import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.web.dto.CreateGroupRequest;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.stream.Collectors;

@Route(value = "groups", layout = MainLayout.class)
@PageTitle("Groups | Mitire")
@RolesAllowed("ADMIN")
public class GroupsView extends VerticalLayout {

    private final GroupService groupService;
    private final Grid<GroupDto> grid = new Grid<>(GroupDto.class, false);

    public GroupsView(GroupService groupService, ProjectService projectService) {
        this.groupService = groupService;

        add(buildForm(projectService));
        add(buildGrid());

        refreshGrid();
    }

    private FormLayout buildForm(ProjectService projectService) {
        TextField name = new TextField("Name");
        MultiSelectComboBox<ProjectDto> projects = new MultiSelectComboBox<>("Accessible projects");
        projects.setItems(projectService.findAll());
        projects.setItemLabelGenerator(p -> p.code() + " - " + p.name());

        Button submit = new Button("Create", e -> {
            if (name.getValue().isBlank()) {
                Notification.show("Name is required").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                var projectIds = projects.getSelectedItems().stream().map(ProjectDto::id).toList();
                groupService.create(new CreateGroupRequest(name.getValue(), projectIds));
                Notification.show("Group created").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                name.clear();
                projects.clear();
                refreshGrid();
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        return new FormLayout(name, projects, submit);
    }

    private Grid<GroupDto> buildGrid() {
        grid.addColumn(GroupDto::name).setHeader("Name").setSortable(true);
        grid.addColumn(g -> g.projects().stream().map(ProjectDto::code).collect(Collectors.joining(", ")))
                .setHeader("Projects");
        grid.setWidthFull();
        grid.setHeight("400px");
        grid.getStyle().set("cursor", "pointer");
        grid.addItemClickListener(e -> UI.getCurrent().navigate(GroupDetailView.class, e.getItem().id()));
        return grid;
    }

    private void refreshGrid() {
        grid.setItems(groupService.findAll());
    }
}
