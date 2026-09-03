package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.service.GroupService;
import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.CreateGroupRequest;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import it.brunasti.mitire.ui.util.Notifications;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.stream.Collectors;

@Route(value = "groups", layout = MainLayout.class)
@PageTitle("Groups | MiTiRe")
@RolesAllowed("ADMIN")
public class GroupsView extends VerticalLayout {

    private final GroupService groupService;
    private final UserService userService;
    private final Grid<GroupDto> grid = new Grid<>(GroupDto.class, false);

    public GroupsView(GroupService groupService, ProjectService projectService, UserService userService) {
        this.groupService = groupService;
        this.userService = userService;

        setSizeFull();

        H2 title = new H2("Groups");

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Groups", buildGrid());
        tabSheet.add("Add Group", buildForm(projectService));
        tabSheet.setSizeFull();

        add(title, tabSheet);
        setFlexGrow(1, tabSheet);

        refreshGrid();
    }

    private FormLayout buildForm(ProjectService projectService) {
        TextField name = new TextField("Name");
        ComboBox<Role> role = new ComboBox<>("Role");
        role.setItems(Role.values());
        role.setValue(Role.MEMBER);
        role.setHelperText("The role members get on this group's projects");
        MultiSelectComboBox<ProjectDto> projects = new MultiSelectComboBox<>("Accessible projects");
        projects.setItems(projectService.findAll());
        projects.setItemLabelGenerator(p -> p.code() + " - " + p.name());

        Button submit = new Button("Create", e -> {
            if (name.getValue().isBlank()) {
                Notifications.showError("Name is required");
                return;
            }
            if (role.getValue() == null) {
                Notifications.showError("Role is required");
                return;
            }
            try {
                var projectIds = projects.getSelectedItems().stream().map(ProjectDto::id).toList();
                groupService.create(new CreateGroupRequest(name.getValue(), role.getValue(), projectIds));
                Notification.show("Group created").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                name.clear();
                role.setValue(Role.MEMBER);
                projects.clear();
                refreshGrid();
            } catch (IllegalArgumentException ex) {
                Notifications.showError(ex.getMessage());
            }
        });

        FormLayout form = new FormLayout(name, role, projects, submit);
        form.setMaxWidth("600px");
        return form;
    }

    private Grid<GroupDto> buildGrid() {
        grid.addColumn(GroupDto::name).setHeader("Name").setSortable(true);
        grid.addColumn(GroupDto::role).setHeader("Role").setSortable(true);
        grid.addColumn(g -> g.projects().stream().map(ProjectDto::code).collect(Collectors.joining(", ")))
                .setHeader("Projects").setSortable(true);
        grid.addColumn(g -> userService.findByGroup(g.id()).stream().map(UserDto::username)
                        .collect(Collectors.joining(", ")))
                .setHeader("Users").setSortable(true);
        grid.setSizeFull();
        grid.getStyle().set("cursor", "pointer");
        grid.addItemClickListener(e -> UI.getCurrent().navigate(GroupDetailView.class, e.getItem().id()));
        return grid;
    }

    private void refreshGrid() {
        grid.setItems(groupService.findAll());
    }
}
