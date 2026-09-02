package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.service.GroupService;
import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.service.ProjectEntityStatusService;
import it.brunasti.mitire.backend.service.TimeEntryService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.CreateProjectEntityStatusRequest;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.ProjectEntityStatusDto;
import it.brunasti.mitire.backend.web.dto.TimeEntryDto;
import it.brunasti.mitire.backend.web.dto.UpdateProjectEntityStatusRequest;
import it.brunasti.mitire.backend.web.dto.UpdateProjectRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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
import com.vaadin.flow.spring.security.AuthenticationContext;
import it.brunasti.mitire.ui.util.Notifications;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Route(value = "projects", layout = MainLayout.class)
@PageTitle("Project details | MiTiRe")
@RolesAllowed("ADMIN")
public class ProjectDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ProjectService projectService;
    private final TimeEntryService timeEntryService;
    private final UserService userService;
    private final GroupService groupService;
    private final ProjectEntityStatusService projectEntityStatusService;

    private final TextField code = new TextField("Code");
    private final TextField name = new TextField("Name");
    private final Checkbox active = new Checkbox("Active");
    private final DatePicker startDate = new DatePicker("Start date");
    private final DatePicker endDate = new DatePicker("End date");
    private final ComboBox<UserDto> approver = new ComboBox<>("Approver");
    private final ComboBox<UserDto> owner = new ComboBox<>("Owner");
    private final ComboBox<GroupDto> addGroup = new ComboBox<>("Add group");
    private final Span projectNameLabel = new Span();

    private final Grid<TimeEntryDto> entriesGrid = new Grid<>(TimeEntryDto.class, false);
    private final Grid<UserDto> usersGrid = new Grid<>(UserDto.class, false);
    private final Grid<GroupDto> groupsGrid = new Grid<>(GroupDto.class, false);
    private final Grid<ProjectEntityStatusDto> statusesGrid = new Grid<>(ProjectEntityStatusDto.class, false);

    private Grid.Column<GroupDto> groupActionsColumn;
    private Grid.Column<ProjectEntityStatusDto> statusActionsColumn;

    private final Long currentUserId;

    private List<GroupDto> allGroups;
    private Long projectId;

    public ProjectDetailView(ProjectService projectService, TimeEntryService timeEntryService,
                              UserService userService, GroupService groupService,
                              ProjectEntityStatusService projectEntityStatusService,
                              AuthenticationContext authenticationContext) {
        this.projectService = projectService;
        this.timeEntryService = timeEntryService;
        this.userService = userService;
        this.groupService = groupService;
        this.projectEntityStatusService = projectEntityStatusService;
        this.currentUserId = authenticationContext.getPrincipalName()
                .map(userService::getByUsername)
                .map(UserDto::id)
                .orElseThrow();

        setSizeFull();

        startDate.setWidth("160px");
        endDate.setWidth("160px");
        approver.setItemLabelGenerator(UserDto::fullName);
        approver.setClearButtonVisible(true);
        owner.setItemLabelGenerator(UserDto::fullName);
        owner.setClearButtonVisible(true);

        allGroups = groupService.findAll();
        addGroup.setItemLabelGenerator(GroupDto::name);

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Project details", buildDetailsTab());
        tabSheet.add("Time Entries", buildEntriesTab());
        tabSheet.add("Users", buildUsersTab());
        tabSheet.add("Groups", buildGroupsTab());
        tabSheet.add("Statuses", buildStatusesTab());
        tabSheet.setSizeFull();

        projectNameLabel.getStyle().set("font-weight", "bold").set("margin-left", "1rem");
        HorizontalLayout header = new HorizontalLayout(new RouterLink("← Back to projects", ProjectsView.class), projectNameLabel);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        add(header, tabSheet);
        setFlexGrow(1, tabSheet);
    }

    @Override
    public void setParameter(BeforeEvent event, Long projectId) {
        this.projectId = projectId;
        ProjectDto project;
        try {
            project = projectService.findById(projectId);
            code.setValue(project.code());
            name.setValue(project.name());
            active.setValue(project.active());
            startDate.setValue(project.startDate());
            endDate.setValue(project.endDate());
            projectNameLabel.setText(project.name());
        } catch (NoSuchElementException ex) {
            event.rerouteToError(NotFoundException.class, "Project not found");
            return;
        }
        entriesGrid.setItems(timeEntryService.search(null, projectId, null, null));
        List<UserDto> projectUsers = userService.findByProjectAccess(projectId);
        usersGrid.setItems(projectUsers);
        approver.setItems(projectUsers);
        approver.clear();
        Long approverId = project.approverId();
        if (approverId != null) {
            projectUsers.stream().filter(u -> u.id().equals(approverId)).findFirst().ifPresent(approver::setValue);
        }
        owner.setItems(projectUsers);
        owner.clear();
        Long ownerId = project.ownerId();
        if (ownerId != null) {
            projectUsers.stream().filter(u -> u.id().equals(ownerId)).findFirst().ifPresent(owner::setValue);
        }
        refreshGroups();
        refreshStatuses();
    }

    private FormLayout buildDetailsTab() {
        code.setReadOnly(true);

        Button save = new Button("Save", e -> {
            try {
                Long approverId = approver.getValue() != null ? approver.getValue().id() : null;
                Long ownerId = owner.getValue() != null ? owner.getValue().id() : null;
                ProjectDto updated = projectService.update(projectId, new UpdateProjectRequest(
                        name.getValue(), active.getValue(), startDate.getValue(), endDate.getValue(), approverId, ownerId));
                projectNameLabel.setText(updated.name());
                Notification.show("Project updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                Notifications.showError(ex.getMessage());
            }
        });

        HorizontalLayout startAndEndDate = new HorizontalLayout(startDate, endDate);

        FormLayout form = new FormLayout(code, name, active, startAndEndDate, approver, owner, save);
        form.setMaxWidth("600px");
        return form;
    }

    private VerticalLayout buildEntriesTab() {
        entriesGrid.addColumn(TimeEntryDto::workDate).setHeader("Date").setSortable(true);
        entriesGrid.addColumn(TimeEntryDto::username).setHeader("User").setSortable(true);
        entriesGrid.addColumn(TimeEntryDto::hours).setHeader("Hours");
        entriesGrid.addColumn(TimeEntryDto::description).setHeader("Description");
        entriesGrid.addColumn(TimeEntryDto::statusName).setHeader("Status");
        entriesGrid.setSizeFull();
        entriesGrid.getStyle().set("cursor", "pointer");
        entriesGrid.addItemClickListener(e -> UI.getCurrent().navigate(TimeEntryDetailView.class, e.getItem().id()));

        VerticalLayout layout = new VerticalLayout(entriesGrid);
        layout.setSizeFull();
        return layout;
    }

    private VerticalLayout buildUsersTab() {
        usersGrid.addColumn(UserDto::username).setHeader("Username").setSortable(true);
        usersGrid.addColumn(UserDto::fullName).setHeader("Full name");
        usersGrid.addColumn(UserDto::email).setHeader("Email");
        usersGrid.addColumn(UserDto::role).setHeader("Role");
        usersGrid.addColumn(u -> u.groups().stream().map(GroupDto::name).reduce((a, b) -> a + ", " + b).orElse("-"))
                .setHeader("Groups");
        usersGrid.setSizeFull();
        usersGrid.getStyle().set("cursor", "pointer");
        usersGrid.addItemClickListener(e -> UI.getCurrent().navigate(UserDetailView.class, e.getItem().id()));

        VerticalLayout layout = new VerticalLayout(usersGrid);
        layout.setSizeFull();
        return layout;
    }

    private VerticalLayout buildGroupsTab() {
        Button add = new Button("Add", e -> addGroupLink());
        HorizontalLayout addForm = new HorizontalLayout(addGroup, add);
        addForm.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        groupsGrid.addColumn(GroupDto::name).setHeader("Name").setSortable(true);
        groupsGrid.addColumn(g -> g.projects().stream().map(ProjectDto::code).collect(Collectors.joining(", ")))
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
            Notifications.showError("Select a group to add");
            return;
        }
        groupService.addProject(selected.id(), projectId);
        refreshGroups();
        refreshProjectUsers();
        Notification.show("Group added").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private Button buildRemoveGroupButton(GroupDto group) {
        Button button = new Button(VaadinIcon.TRASH.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        button.setTooltipText("Remove from project");
        button.addClickListener(e -> confirmRemoveGroup(group));
        return button;
    }

    private void confirmRemoveGroup(GroupDto group) {
        ConfirmDialog dialog = new ConfirmDialog(
                "Remove group",
                "Remove '" + group.name() + "' from this project?",
                "Remove", e -> removeGroup(group),
                "Cancel", e -> { }
        );
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    private void removeGroup(GroupDto group) {
        groupService.removeProject(group.id(), projectId);
        refreshGroups();
        refreshProjectUsers();
        Notification.show("Group removed from project").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void refreshGroups() {
        List<GroupDto> linkedGroups = groupService.findByProject(projectId);
        groupsGrid.setItems(linkedGroups);
        addGroup.clear();
        addGroup.setItems(allGroups.stream().filter(g -> !linkedGroups.contains(g)).toList());
    }

    private void refreshProjectUsers() {
        List<UserDto> projectUsers = userService.findByProjectAccess(projectId);
        usersGrid.setItems(projectUsers);
        UserDto currentApprover = approver.getValue();
        approver.setItems(projectUsers);
        if (currentApprover != null) {
            projectUsers.stream().filter(u -> u.id().equals(currentApprover.id())).findFirst()
                    .ifPresentOrElse(approver::setValue, approver::clear);
        }
        UserDto currentOwner = owner.getValue();
        owner.setItems(projectUsers);
        if (currentOwner != null) {
            projectUsers.stream().filter(u -> u.id().equals(currentOwner.id())).findFirst()
                    .ifPresentOrElse(owner::setValue, owner::clear);
        }
    }

    private VerticalLayout buildStatusesTab() {
        TextField newStatusName = new TextField("Name");
        TextField newStatusDescription = new TextField("Description");
        Button add = new Button("Add", e -> addStatus(newStatusName, newStatusDescription));
        HorizontalLayout addForm = new HorizontalLayout(newStatusName, newStatusDescription, add);
        addForm.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        statusesGrid.addColumn(ProjectEntityStatusDto::sequence).setHeader("Order");
        statusesGrid.addColumn(ProjectEntityStatusDto::name).setHeader("Name");
        statusesGrid.addColumn(ProjectEntityStatusDto::description).setHeader("Description");
        statusesGrid.addColumn(ProjectEntityStatusDto::active).setHeader("Active");
        statusesGrid.addColumn(ProjectEntityStatusDto::startingStatus).setHeader("Starting");
        statusActionsColumn = statusesGrid.addComponentColumn(this::buildStatusActions).setHeader("").setFlexGrow(0);
        statusesGrid.setSizeFull();
        statusesGrid.getStyle().set("cursor", "pointer");
        statusesGrid.addItemClickListener(e -> {
            if (e.getColumn() != statusActionsColumn) {
                UI.getCurrent().navigate(ProjectEntityStatusDetailView.class, e.getItem().id());
            }
        });

        VerticalLayout layout = new VerticalLayout(addForm, statusesGrid);
        layout.setSizeFull();
        return layout;
    }

    private void addStatus(TextField newStatusName, TextField newStatusDescription) {
        if (newStatusName.getValue().isBlank()) {
            Notifications.showError("Name is required");
            return;
        }
        try {
            projectEntityStatusService.create(projectId,
                    new CreateProjectEntityStatusRequest(newStatusName.getValue(), newStatusDescription.getValue()),
                    currentUserId);
            newStatusName.clear();
            newStatusDescription.clear();
            refreshStatuses();
            Notification.show("Status added").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (IllegalArgumentException ex) {
            Notifications.showError(ex.getMessage());
        }
    }

    private HorizontalLayout buildStatusActions(ProjectEntityStatusDto status) {
        Button up = new Button(VaadinIcon.ARROW_UP.create());
        up.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        up.setTooltipText("Move up");
        up.addClickListener(e -> {
            projectEntityStatusService.moveUp(projectId, status.id(), currentUserId);
            refreshStatuses();
        });

        Button down = new Button(VaadinIcon.ARROW_DOWN.create());
        down.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        down.setTooltipText("Move down");
        down.addClickListener(e -> {
            projectEntityStatusService.moveDown(projectId, status.id(), currentUserId);
            refreshStatuses();
        });

        Button edit = new Button(VaadinIcon.EDIT.create());
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        edit.setTooltipText("Edit status");
        edit.addClickListener(e -> openEditStatusDialog(status));

        Button delete = new Button(VaadinIcon.TRASH.create());
        delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        delete.setTooltipText("Delete status");
        delete.addClickListener(e -> confirmDeleteStatus(status));

        HorizontalLayout actions = new HorizontalLayout(up, down, edit, delete);

        if (status.startingStatus()) {
            Icon starringIcon = VaadinIcon.STAR.create();
            starringIcon.setColor("var(--lumo-primary-color)");
            starringIcon.setTooltipText("This is the starting status");
            actions.addComponentAsFirst(starringIcon);
        } else {
            Button setStarting = new Button(VaadinIcon.STAR_O.create());
            setStarting.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
            setStarting.setTooltipText("Set as starting status");
            setStarting.addClickListener(e -> {
                projectEntityStatusService.setStarting(projectId, status.id(), currentUserId);
                refreshStatuses();
            });
            actions.addComponentAsFirst(setStarting);
        }

        return actions;
    }

    private void openEditStatusDialog(ProjectEntityStatusDto status) {
        TextField editName = new TextField("Name");
        editName.setValue(status.name());
        TextField editDescription = new TextField("Description");
        editDescription.setValue(status.description() != null ? status.description() : "");
        Checkbox editActive = new Checkbox("Active");
        editActive.setValue(status.active());

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit status");

        Button save = new Button("Save", e -> {
            try {
                projectEntityStatusService.update(projectId, status.id(), new UpdateProjectEntityStatusRequest(
                        editName.getValue(), editDescription.getValue(), editActive.getValue()), currentUserId);
                refreshStatuses();
                dialog.close();
                Notification.show("Status updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                Notifications.showError(ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(new FormLayout(editName, editDescription, editActive));
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDeleteStatus(ProjectEntityStatusDto status) {
        ConfirmDialog dialog = new ConfirmDialog(
                "Delete status",
                "Delete status '" + status.name() + "'? This can't be undone.",
                "Delete", e -> deleteStatus(status),
                "Cancel", e -> { }
        );
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    private void deleteStatus(ProjectEntityStatusDto status) {
        try {
            projectEntityStatusService.delete(projectId, status.id(), currentUserId);
            refreshStatuses();
            Notification.show("Status deleted").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (IllegalArgumentException ex) {
            Notifications.showError(ex.getMessage());
        }
    }

    private void refreshStatuses() {
        statusesGrid.setItems(projectEntityStatusService.findByProject(projectId));
    }
}
