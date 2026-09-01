package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.service.GroupService;
import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.web.dto.CreateGroupRequest;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.UpdateGroupRequest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.HashSet;
import java.util.stream.Collectors;

@Route(value = "groups", layout = MainLayout.class)
@PageTitle("Groups | Mitire")
@RolesAllowed("ADMIN")
public class GroupsView extends VerticalLayout {

    private final GroupService groupService;
    private final Grid<GroupDto> grid = new Grid<>(GroupDto.class, false);

    private final TextField name = new TextField("Name");
    private final MultiSelectComboBox<ProjectDto> projects = new MultiSelectComboBox<>("Accessible projects");
    private final Button submit = new Button("Create");
    private final Button cancel = new Button("Cancel");

    private Long editingId;

    public GroupsView(GroupService groupService, ProjectService projectService) {
        this.groupService = groupService;

        projects.setItems(projectService.findAll());
        projects.setItemLabelGenerator(p -> p.code() + " - " + p.name());

        cancel.setVisible(false);
        submit.addClickListener(e -> save());
        cancel.addClickListener(e -> resetForm());

        FormLayout form = new FormLayout(name, projects, new HorizontalLayout(submit, cancel));
        add(form);
        add(buildGrid());

        refreshGrid();
    }

    private void save() {
        if (name.getValue().isBlank()) {
            Notification.show("Name is required").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        var projectIds = projects.getSelectedItems().stream().map(ProjectDto::id).toList();
        try {
            if (editingId == null) {
                groupService.create(new CreateGroupRequest(name.getValue(), projectIds));
                Notification.show("Group created").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                groupService.update(editingId, new UpdateGroupRequest(name.getValue(), projectIds));
                Notification.show("Group updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            resetForm();
            refreshGrid();
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void resetForm() {
        editingId = null;
        name.clear();
        projects.clear();
        submit.setText("Create");
        cancel.setVisible(false);
    }

    private void edit(GroupDto group) {
        editingId = group.id();
        name.setValue(group.name());
        projects.setValue(new HashSet<>(group.projects()));
        submit.setText("Update");
        cancel.setVisible(true);
    }

    private Grid<GroupDto> buildGrid() {
        grid.addColumn(GroupDto::name).setHeader("Name").setSortable(true);
        grid.addColumn(g -> g.projects().stream().map(ProjectDto::code).collect(Collectors.joining(", ")))
                .setHeader("Projects");
        grid.addComponentColumn(g -> new Button("Edit", e -> edit(g))).setHeader("");
        grid.setWidthFull();
        grid.setHeight("400px");
        return grid;
    }

    private void refreshGrid() {
        grid.setItems(groupService.findAll());
    }
}
