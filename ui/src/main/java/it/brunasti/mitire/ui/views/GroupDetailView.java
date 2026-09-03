package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.service.GroupService;
import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.UpdateGroupRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import it.brunasti.mitire.ui.util.Notifications;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.NoSuchElementException;

@Route(value = "groups", layout = MainLayout.class)
@PageTitle("Group details | MiTiRe")
@RolesAllowed("ADMIN")
public class GroupDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final GroupService groupService;
    private final UserService userService;

    private final TextField name = new TextField("Name");
    private final ComboBox<Role> role = new ComboBox<>("Role");
    private final Span groupNameLabel = new Span();
    private final ComboBox<ProjectDto> addProject = new ComboBox<>("Add project");
    private final ComboBox<UserDto> addUser = new ComboBox<>("Add user");
    private final Grid<ProjectDto> projectsGrid = new Grid<>(ProjectDto.class, false);
    private final Grid<UserDto> usersGrid = new Grid<>(UserDto.class, false);

    private Grid.Column<ProjectDto> projectActionsColumn;
    private Grid.Column<UserDto> userActionsColumn;

    private List<ProjectDto> allProjects;
    private List<UserDto> allUsers;
    private Long groupId;
    private List<Long> currentProjectIds = List.of();

    public GroupDetailView(GroupService groupService, ProjectService projectService, UserService userService) {
        this.groupService = groupService;
        this.userService = userService;

        setSizeFull();

        role.setItems(Role.values());
        role.setHelperText("The role members get on this group's projects");

        allProjects = projectService.findAll();
        addProject.setItemLabelGenerator(p -> p.code() + " - " + p.name());

        allUsers = userService.findAll();
        addUser.setItemLabelGenerator(UserDto::username);

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Group details", buildDetailsTab());
        tabSheet.add("Projects", buildProjectsTab());
        tabSheet.add("Users", buildUsersTab());
        tabSheet.setSizeFull();

        groupNameLabel.getStyle().set("font-weight", "bold").set("margin-left", "1rem");
        HorizontalLayout header = new HorizontalLayout(new RouterLink("← Back to groups", GroupsView.class), groupNameLabel);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        add(header, tabSheet);
        setFlexGrow(1, tabSheet);
    }

    @Override
    public void setParameter(BeforeEvent event, Long groupId) {
        this.groupId = groupId;
        GroupDto group;
        try {
            group = groupService.findById(groupId);
        } catch (NoSuchElementException ex) {
            event.rerouteToError(NotFoundException.class, "Group not found");
            return;
        }
        name.setValue(group.name());
        role.setValue(group.role());
        groupNameLabel.setText(group.name());
        refreshProjects(group.projects());
        refreshUsers(userService.findByGroup(groupId));
    }

    private FormLayout buildDetailsTab() {
        Button save = new Button("Save", e -> save());
        FormLayout form = new FormLayout(name, role, save);
        form.setMaxWidth("600px");
        return form;
    }

    private VerticalLayout buildProjectsTab() {
        Button add = new Button("Add", e -> addProjectLink());
        HorizontalLayout addForm = new HorizontalLayout(addProject, add);
        addForm.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        projectsGrid.addColumn(ProjectDto::code).setHeader("Code").setSortable(true).setAutoWidth(true).setFlexGrow(0);
        projectsGrid.addColumn(ProjectDto::name).setHeader("Name");
        projectsGrid.addColumn(ProjectDto::active).setHeader("Active").setAutoWidth(true).setFlexGrow(0);
        projectActionsColumn = projectsGrid.addComponentColumn(this::buildRemoveProjectButton).setHeader("").setFlexGrow(0);
        projectsGrid.setSizeFull();
        projectsGrid.getStyle().set("cursor", "pointer");
        projectsGrid.addItemClickListener(e -> {
            if (e.getColumn() != projectActionsColumn) {
                UI.getCurrent().navigate(ProjectDetailView.class, e.getItem().id());
            }
        });

        VerticalLayout layout = new VerticalLayout(addForm, projectsGrid);
        layout.setSizeFull();
        return layout;
    }

    private VerticalLayout buildUsersTab() {
        Button add = new Button("Add", e -> addUserLink());
        HorizontalLayout addForm = new HorizontalLayout(addUser, add);
        addForm.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        usersGrid.addColumn(UserDto::username).setHeader("Username").setSortable(true);
        usersGrid.addColumn(UserDto::fullName).setHeader("Full name");
        usersGrid.addColumn(UserDto::email).setHeader("Email");
        usersGrid.addColumn(UserDto::role).setHeader("Role").setAutoWidth(true).setFlexGrow(0);
        usersGrid.addColumn(UserDto::enabled).setHeader("Enabled").setAutoWidth(true).setFlexGrow(0);
        userActionsColumn = usersGrid.addComponentColumn(this::buildRemoveUserButton).setHeader("").setFlexGrow(0);
        usersGrid.setSizeFull();
        usersGrid.getStyle().set("cursor", "pointer");
        usersGrid.addItemClickListener(e -> {
            if (e.getColumn() != userActionsColumn) {
                UI.getCurrent().navigate(UserDetailView.class, e.getItem().id());
            }
        });

        VerticalLayout layout = new VerticalLayout(addForm, usersGrid);
        layout.setSizeFull();
        return layout;
    }

    private Button buildRemoveProjectButton(ProjectDto project) {
        Button button = new Button(VaadinIcon.TRASH.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        button.setTooltipText("Remove from group");
        button.addClickListener(e -> confirmRemoveProject(project));
        return button;
    }

    private Button buildRemoveUserButton(UserDto user) {
        Button button = new Button(VaadinIcon.TRASH.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        button.setTooltipText("Remove from group");
        button.addClickListener(e -> confirmRemoveUser(user));
        return button;
    }

    private void confirmRemoveProject(ProjectDto project) {
        ConfirmDialog dialog = new ConfirmDialog(
                "Remove project",
                "Remove '" + project.code() + " - " + project.name() + "' from this group?",
                "Remove", e -> removeProject(project),
                "Cancel", e -> { }
        );
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    private void confirmRemoveUser(UserDto user) {
        ConfirmDialog dialog = new ConfirmDialog(
                "Remove user",
                "Remove '" + user.username() + "' from this group?",
                "Remove", e -> removeUser(user),
                "Cancel", e -> { }
        );
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    private void removeProject(ProjectDto project) {
        GroupDto updated = groupService.removeProject(groupId, project.id());
        refreshProjects(updated.projects());
        Notification.show("Project removed from group").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void removeUser(UserDto user) {
        userService.removeFromGroup(user.id(), groupId);
        refreshUsers(userService.findByGroup(groupId));
        Notification.show("User removed from group").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void addProjectLink() {
        ProjectDto selected = addProject.getValue();
        if (selected == null) {
            Notifications.showError("Select a project to add");
            return;
        }
        GroupDto updated = groupService.addProject(groupId, selected.id());
        refreshProjects(updated.projects());
        Notification.show("Project added").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void addUserLink() {
        UserDto selected = addUser.getValue();
        if (selected == null) {
            Notifications.showError("Select a user to add");
            return;
        }
        userService.addToGroup(selected.id(), groupId);
        refreshUsers(userService.findByGroup(groupId));
        Notification.show("User added").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void refreshProjects(List<ProjectDto> linkedProjects) {
        currentProjectIds = linkedProjects.stream().map(ProjectDto::id).toList();
        projectsGrid.setItems(linkedProjects);
        addProject.clear();
        addProject.setItems(allProjects.stream().filter(p -> !linkedProjects.contains(p)).toList());
    }

    private void refreshUsers(List<UserDto> members) {
        usersGrid.setItems(members);
        addUser.clear();
        addUser.setItems(allUsers.stream().filter(u -> members.stream().noneMatch(m -> m.id().equals(u.id()))).toList());
    }

    private void save() {
        if (name.getValue().isBlank()) {
            Notifications.showError("Name is required");
            return;
        }
        if (role.getValue() == null) {
            Notifications.showError("Role is required");
            return;
        }
        try {
            GroupDto updated = groupService.update(groupId,
                    new UpdateGroupRequest(name.getValue(), role.getValue(), currentProjectIds));
            groupNameLabel.setText(updated.name());
            Notification.show("Group updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (IllegalArgumentException ex) {
            Notifications.showError(ex.getMessage());
        }
    }
}
