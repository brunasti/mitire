package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.service.GroupService;
import it.brunasti.mitire.backend.service.TimeEntryService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.TimeEntryDto;
import it.brunasti.mitire.backend.web.dto.UpdateUserRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("User details | MiTiRe")
@RolesAllowed("ADMIN")
public class UserDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final UserService userService;
    private final TimeEntryService timeEntryService;

    private final Span userNameLabel = new Span();
    private final TextField username = new TextField("Username");
    private final TextField fullName = new TextField("Full name");
    private final TextField email = new TextField("Email");
    private final PasswordField password = new PasswordField("Password");
    private final ComboBox<Role> role = new ComboBox<>("Role");
    private final MultiSelectComboBox<GroupDto> groups = new MultiSelectComboBox<>("Groups");
    private final Checkbox enabled = new Checkbox("Enabled");

    private final ComboBox<GroupDto> addGroup = new ComboBox<>("Add group");
    private final Grid<TimeEntryDto> entriesGrid = new Grid<>(TimeEntryDto.class, false);
    private final Grid<GroupDto> groupsGrid = new Grid<>(GroupDto.class, false);
    private final Grid<ProjectDto> projectsGrid = new Grid<>(ProjectDto.class, false);

    private Grid.Column<GroupDto> groupActionsColumn;

    private List<GroupDto> allGroups;
    private Long userId;
    private Role currentRole;

    public UserDetailView(UserService userService, GroupService groupService, TimeEntryService timeEntryService) {
        this.userService = userService;
        this.timeEntryService = timeEntryService;

        setSizeFull();

        username.setReadOnly(true);
        role.setItems(Role.values());
        allGroups = groupService.findAll();
        groups.setItems(allGroups);
        groups.setItemLabelGenerator(GroupDto::name);
        addGroup.setItemLabelGenerator(GroupDto::name);

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("User details", buildDetailsTab());
        tabSheet.add("Time recordings", buildEntriesTab());
        tabSheet.add("Groups", buildGroupsTab());
        tabSheet.add("Projects", buildProjectsTab());
        tabSheet.setSizeFull();

        userNameLabel.getStyle().set("font-weight", "bold").set("margin-left", "1rem");
        HorizontalLayout header = new HorizontalLayout(new RouterLink("← Back to users", UsersView.class), userNameLabel);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        add(header, tabSheet);
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

        userNameLabel.setText(user.fullName());
        username.setValue(user.username());
        fullName.setValue(user.fullName());
        email.setValue(user.email());
        password.clear();
        updatePasswordFieldState();
        role.setValue(user.role());
        groups.setValue(new HashSet<>(user.groups()));
        enabled.setValue(user.enabled());

        entriesGrid.setItems(timeEntryService.search(userId, null, null, null));
        refreshComputedTabs(user);
    }

    private FormLayout buildDetailsTab() {
        password.setHelperText("Leave blank to keep the current password.");

        Button save = new Button("Save", e -> save());
        FormLayout form = new FormLayout(username, fullName, email, password, role, groups, enabled, save);
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
        Button add = new Button("Add", e -> addGroupLink());
        HorizontalLayout addForm = new HorizontalLayout(addGroup, add);
        addForm.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        groupsGrid.addColumn(GroupDto::name).setHeader("Name").setSortable(true);
        groupsGrid.addColumn(g -> g.projects().stream().map(ProjectDto::code)
                        .reduce((a, b) -> a + ", " + b).orElse(""))
                .setHeader("Projects");
        groupActionsColumn = groupsGrid.addComponentColumn(this::buildRemoveGroupButton).setHeader("").setFlexGrow(0);
        groupsGrid.setSizeFull();
        groupsGrid.getStyle().set("cursor", "pointer");
        groupsGrid.addItemClickListener(e -> {
            if (e.getColumn() != groupActionsColumn) {
                UI.getCurrent().navigate(GroupDetailView.class, e.getItem().id());
            }
        });

        VerticalLayout layout = new VerticalLayout(addForm, groupsGrid);
        layout.setSizeFull();
        return layout;
    }

    private void addGroupLink() {
        GroupDto selected = addGroup.getValue();
        if (selected == null) {
            Notification.show("Select a group to add").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        UserDto updated = userService.addToGroup(userId, selected.id());
        groups.setValue(new HashSet<>(updated.groups()));
        addGroup.clear();
        refreshComputedTabs(updated);
        Notification.show("Group added").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private Button buildRemoveGroupButton(GroupDto group) {
        Button button = new Button(VaadinIcon.TRASH.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        button.setTooltipText("Remove from user");
        button.addClickListener(e -> confirmRemoveGroup(group));
        return button;
    }

    private void confirmRemoveGroup(GroupDto group) {
        ConfirmDialog dialog = new ConfirmDialog(
                "Remove group",
                "Remove '" + group.name() + "' from this user?",
                "Remove", e -> removeGroup(group),
                "Cancel", e -> { }
        );
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    private void removeGroup(GroupDto group) {
        UserDto updated = userService.removeFromGroup(userId, group.id());
        groups.setValue(new HashSet<>(updated.groups()));
        refreshComputedTabs(updated);
        Notification.show("Group removed from user").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
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
        var groupIds = groups.getSelectedItems().stream().map(GroupDto::id).toList();
        try {
            if (!password.getValue().isBlank() && currentRole != Role.ADMIN) {
                userService.updatePassword(userId, password.getValue());
            }
            UserDto updated = userService.update(userId, new UpdateUserRequest(fullName.getValue(), email.getValue(),
                    role.getValue(), groupIds, enabled.getValue()));
            password.clear();
            userNameLabel.setText(updated.fullName());
            currentRole = updated.role();
            updatePasswordFieldState();
            refreshComputedTabs(updated);
            Notification.show("User updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (IllegalArgumentException | AccessDeniedException ex) {
            Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void refreshComputedTabs(UserDto user) {
        groupsGrid.setItems(user.groups());
        addGroup.setItems(allGroups.stream().filter(g -> !user.groups().contains(g)).toList());
        projectsGrid.setItems(userService.findAccessibleProjects(userId));
    }

    private void updatePasswordFieldState() {
        password.setEnabled(currentRole != Role.ADMIN);
        password.setHelperText(currentRole == Role.ADMIN
                ? "An ADMIN user's password can't be changed here."
                : "Leave blank to keep the current password.");
    }
}
