package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.service.GroupService;
import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.service.TimeEntryService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.TimeEntryDto;
import it.brunasti.mitire.backend.web.dto.UpdateUserRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.NoSuchElementException;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("User details | Mitire")
@RolesAllowed("ADMIN")
public class UserDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final UserService userService;
    private final GroupService groupService;
    private final ProjectService projectService;
    private final TimeEntryService timeEntryService;

    private final TextField username = new TextField("Username");
    private final TextField fullName = new TextField("Full name");
    private final TextField email = new TextField("Email");
    private final PasswordField password = new PasswordField("Password");
    private final ComboBox<Role> role = new ComboBox<>("Role");
    private final ComboBox<GroupDto> group = new ComboBox<>("Group");
    private final Checkbox enabled = new Checkbox("Enabled");

    private final Grid<TimeEntryDto> entriesGrid = new Grid<>(TimeEntryDto.class, false);
    private final Grid<GroupDto> groupsGrid = new Grid<>(GroupDto.class, false);
    private final Grid<ProjectDto> projectsGrid = new Grid<>(ProjectDto.class, false);

    private List<GroupDto> groups;
    private Long userId;
    private Role currentRole;

    public UserDetailView(UserService userService, GroupService groupService,
                           ProjectService projectService, TimeEntryService timeEntryService) {
        this.userService = userService;
        this.groupService = groupService;
        this.projectService = projectService;
        this.timeEntryService = timeEntryService;

        setSizeFull();

        username.setReadOnly(true);
        role.setItems(Role.values());
        groups = groupService.findAll();
        group.setItems(groups);
        group.setItemLabelGenerator(GroupDto::name);
        group.setClearButtonVisible(true);

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("User details", buildDetailsTab());
        tabSheet.add("Time recordings", buildEntriesTab());
        tabSheet.add("Groups", buildGroupsTab());
        tabSheet.add("Projects", buildProjectsTab());
        tabSheet.setSizeFull();

        add(new RouterLink("← Back to users", UsersView.class), tabSheet);
        setFlexGrow(1, tabSheet);
    }

    @Override
    public void setParameter(BeforeEvent event, Long userId) {
        this.userId = userId;
        UserDto user;
        try {
            user = userService.findById(userId);
        } catch (NoSuchElementException ex) {
            event.rerouteToError(NotFoundException.class, "User not found");
            return;
        }
        currentRole = user.role();

        username.setValue(user.username());
        fullName.setValue(user.fullName());
        email.setValue(user.email());
        password.clear();
        updatePasswordFieldState();
        role.setValue(user.role());
        group.setValue(findGroup(user.groupId()));
        enabled.setValue(user.enabled());

        entriesGrid.setItems(timeEntryService.search(userId, null, null, null));
        refreshComputedTabs(user);
    }

    private FormLayout buildDetailsTab() {
        password.setHelperText("Leave blank to keep the current password.");

        Button save = new Button("Save", e -> save());
        FormLayout form = new FormLayout(username, fullName, email, password, role, group, enabled, save);
        form.setMaxWidth("600px");
        return form;
    }

    private VerticalLayout buildEntriesTab() {
        entriesGrid.addColumn(TimeEntryDto::workDate).setHeader("Date").setSortable(true);
        entriesGrid.addColumn(TimeEntryDto::projectCode).setHeader("Project").setSortable(true);
        entriesGrid.addColumn(TimeEntryDto::hours).setHeader("Hours");
        entriesGrid.addColumn(TimeEntryDto::description).setHeader("Description");
        entriesGrid.addColumn(TimeEntryDto::status).setHeader("Status");
        entriesGrid.setSizeFull();

        VerticalLayout layout = new VerticalLayout(entriesGrid);
        layout.setSizeFull();
        return layout;
    }

    private VerticalLayout buildGroupsTab() {
        groupsGrid.addColumn(GroupDto::name).setHeader("Name").setSortable(true);
        groupsGrid.addColumn(g -> g.projects().stream().map(ProjectDto::code)
                        .reduce((a, b) -> a + ", " + b).orElse(""))
                .setHeader("Projects");
        groupsGrid.setSizeFull();
        groupsGrid.getStyle().set("cursor", "pointer");
        groupsGrid.addItemClickListener(e -> UI.getCurrent().navigate(GroupDetailView.class, e.getItem().id()));

        VerticalLayout layout = new VerticalLayout(groupsGrid);
        layout.setSizeFull();
        return layout;
    }

    private VerticalLayout buildProjectsTab() {
        projectsGrid.addColumn(ProjectDto::code).setHeader("Code").setSortable(true);
        projectsGrid.addColumn(ProjectDto::name).setHeader("Name");
        projectsGrid.addColumn(ProjectDto::active).setHeader("Active");
        projectsGrid.setSizeFull();
        projectsGrid.getStyle().set("cursor", "pointer");
        projectsGrid.addItemClickListener(e -> UI.getCurrent().navigate(ProjectDetailView.class, e.getItem().id()));

        VerticalLayout layout = new VerticalLayout(projectsGrid);
        layout.setSizeFull();
        return layout;
    }

    private void save() {
        if (fullName.getValue().isBlank() || email.getValue().isBlank() || role.getValue() == null) {
            Notification.show("Full name, email and role are required").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        Long groupId = group.getValue() != null ? group.getValue().id() : null;
        try {
            if (!password.getValue().isBlank() && currentRole != Role.ADMIN) {
                userService.updatePassword(userId, password.getValue());
            }
            UserDto updated = userService.update(userId, new UpdateUserRequest(fullName.getValue(), email.getValue(),
                    role.getValue(), groupId, enabled.getValue()));
            password.clear();
            currentRole = updated.role();
            updatePasswordFieldState();
            refreshComputedTabs(updated);
            Notification.show("User updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (IllegalArgumentException | AccessDeniedException ex) {
            Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void refreshComputedTabs(UserDto user) {
        GroupDto currentGroup = findGroup(user.groupId());
        groupsGrid.setItems(currentGroup != null ? List.of(currentGroup) : List.of());

        List<ProjectDto> accessibleProjects = user.role() == Role.ADMIN
                ? projectService.findAll()
                : groupService.findProjectsForGroup(user.groupId());
        projectsGrid.setItems(accessibleProjects);
    }

    private void updatePasswordFieldState() {
        password.setEnabled(currentRole != Role.ADMIN);
        password.setHelperText(currentRole == Role.ADMIN
                ? "An ADMIN user's password can't be changed here."
                : "Leave blank to keep the current password.");
    }

    private GroupDto findGroup(Long groupId) {
        return groupId == null ? null : groups.stream().filter(g -> g.id().equals(groupId)).findFirst().orElse(null);
    }
}
