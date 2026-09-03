package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.service.GroupService;
import it.brunasti.mitire.backend.service.ProjectEntryStatusService;
import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.service.TimeEntryService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.CreateProjectEntryStatusRequest;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.ProjectEntryStatusDto;
import it.brunasti.mitire.backend.web.dto.TimeEntryDto;
import it.brunasti.mitire.backend.web.dto.UpdateProjectEntryStatusRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
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
import it.brunasti.mitire.ui.util.Formatters;
import it.brunasti.mitire.ui.util.Notifications;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Project page for a project's Owner (or ADMIN), separate from the ADMIN-only
 * /projects/{id} page — a project Owner who isn't an ADMIN has no access to that page.
 * Mirrors its tabs (Project details, Time Entries, Users, Groups, Workflow) but only the
 * Workflow tab is editable; the rest are read-only, since an Owner manages the approval
 * workflow, not the project's membership or settings.
 */
@Route(value = "project-workflow", layout = MainLayout.class)
@PageTitle("Project workflow | MiTiRe")
@PermitAll
public class ProjectWorkflowView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ProjectService projectService;
    private final TimeEntryService timeEntryService;
    private final UserService userService;
    private final GroupService groupService;
    private final ProjectEntryStatusService projectEntryStatusService;
    private final Long currentUserId;
    private final Role currentUserRole;

    private final TextField code = new TextField("Code");
    private final TextField name = new TextField("Name");
    private final Checkbox active = new Checkbox("Active");
    private final DatePicker startDate = new DatePicker("Start date");
    private final DatePicker endDate = new DatePicker("End date");
    private final ComboBox<UserDto> approver = new ComboBox<>("Approver");
    private final ComboBox<UserDto> owner = new ComboBox<>("Owner");
    private final H2 projectNameLabel = new H2();

    private final Grid<TimeEntryDto> entriesGrid = new Grid<>(TimeEntryDto.class, false);
    private final Grid<UserDto> usersGrid = new Grid<>(UserDto.class, false);
    private final Grid<GroupDto> groupsGrid = new Grid<>(GroupDto.class, false);
    private final Grid<ProjectEntryStatusDto> statusesGrid = new Grid<>(ProjectEntryStatusDto.class, false);

    private Grid.Column<ProjectEntryStatusDto> statusActionsColumn;

    private Long projectId;
    private Long currentApproverId;
    private Long currentOwnerId;

    public ProjectWorkflowView(ProjectService projectService, TimeEntryService timeEntryService,
                                UserService userService, GroupService groupService,
                                ProjectEntryStatusService projectEntryStatusService,
                                AuthenticationContext authenticationContext) {
        this.projectService = projectService;
        this.timeEntryService = timeEntryService;
        this.userService = userService;
        this.groupService = groupService;
        this.projectEntryStatusService = projectEntryStatusService;
        UserDto currentUser = authenticationContext.getPrincipalName()
                .map(userService::getByUsername)
                .orElseThrow();
        this.currentUserId = currentUser.id();
        this.currentUserRole = currentUser.role();

        setSizeFull();

        code.setReadOnly(true);
        name.setReadOnly(true);
        active.setReadOnly(true);
        startDate.setReadOnly(true);
        startDate.setWidth("160px");
        endDate.setReadOnly(true);
        endDate.setWidth("160px");
        approver.setReadOnly(true);
        approver.setItemLabelGenerator(UserDto::fullName);
        owner.setReadOnly(true);
        owner.setItemLabelGenerator(UserDto::fullName);

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Project details", buildDetailsTab());
        tabSheet.add("Time Entries", buildEntriesTab());
        tabSheet.add("Users", buildUsersTab());
        tabSheet.add("Groups", buildGroupsTab());
        tabSheet.add("Workflow", buildWorkflowTab());
        tabSheet.setSizeFull();

        projectNameLabel.getStyle().set("margin", "0 0 0 1rem");
        HorizontalLayout header = new HorizontalLayout(
                new RouterLink("← Back to my projects", MyProjectsView.class), projectNameLabel);
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
        } catch (NoSuchElementException ex) {
            event.rerouteToError(NotFoundException.class, "Project not found");
            return;
        }
        boolean isOwner = project.ownerId() != null && project.ownerId().equals(currentUserId);
        if (currentUserRole != Role.ADMIN && !isOwner) {
            event.rerouteToError(AccessDeniedException.class);
            return;
        }
        code.setValue(project.code());
        name.setValue(project.name());
        active.setValue(project.active());
        startDate.setValue(project.startDate());
        endDate.setValue(project.endDate());
        projectNameLabel.setText(project.name());

        currentApproverId = project.approverId();
        currentOwnerId = project.ownerId();
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
        entriesGrid.setItems(timeEntryService.search(null, projectId, null, null));
        groupsGrid.setItems(groupService.findByProject(projectId));
        refreshStatuses();
    }

    private FormLayout buildDetailsTab() {
        HorizontalLayout startAndEndDate = new HorizontalLayout(startDate, endDate);
        FormLayout form = new FormLayout(code, name, active, startAndEndDate, approver, owner);
        form.setMaxWidth("600px");
        return form;
    }

    private VerticalLayout buildEntriesTab() {
        entriesGrid.addColumn(TimeEntryDto::workDate).setHeader("Date").setSortable(true).setAutoWidth(true).setFlexGrow(0);
        entriesGrid.addColumn(TimeEntryDto::username).setHeader("User").setSortable(true);
        entriesGrid.addColumn(TimeEntryDto::hours).setHeader("Hours").setAutoWidth(true).setFlexGrow(0);
        entriesGrid.addColumn(TimeEntryDto::description).setHeader("Description");
        entriesGrid.addColumn(TimeEntryDto::statusName).setHeader("Status");
        entriesGrid.addColumn(e -> Formatters.timestamp(e.createdAt())).setHeader("Created").setSortable(true)
                .setAutoWidth(true).setFlexGrow(0);
        entriesGrid.setSizeFull();

        VerticalLayout layout = new VerticalLayout(entriesGrid);
        layout.setSizeFull();
        return layout;
    }

    private VerticalLayout buildUsersTab() {
        usersGrid.addColumn(UserDto::username).setHeader("Username").setSortable(true);
        usersGrid.addColumn(UserDto::fullName).setHeader("Full name");
        usersGrid.addColumn(UserDto::email).setHeader("Email");
        usersGrid.addColumn(UserDto::role).setHeader("Role").setAutoWidth(true).setFlexGrow(0);
        usersGrid.addColumn(u -> u.groups().stream().map(GroupDto::name).reduce((a, b) -> a + ", " + b).orElse("-"))
                .setHeader("Groups");
        usersGrid.addColumn(u -> currentApproverId != null && currentApproverId.equals(u.id()) ? "Yes" : "")
                .setHeader("Approver").setAutoWidth(true).setFlexGrow(0);
        usersGrid.addColumn(u -> currentOwnerId != null && currentOwnerId.equals(u.id()) ? "Yes" : "")
                .setHeader("Owner").setAutoWidth(true).setFlexGrow(0);
        usersGrid.setSizeFull();

        VerticalLayout layout = new VerticalLayout(usersGrid);
        layout.setSizeFull();
        return layout;
    }

    private VerticalLayout buildGroupsTab() {
        groupsGrid.addColumn(GroupDto::name).setHeader("Name").setSortable(true);
        groupsGrid.addColumn(GroupDto::role).setHeader("Role").setAutoWidth(true).setFlexGrow(0);
        groupsGrid.addColumn(g -> g.projects().stream().map(ProjectDto::code).collect(Collectors.joining(", ")))
                .setHeader("Projects");
        groupsGrid.setSizeFull();

        VerticalLayout layout = new VerticalLayout(groupsGrid);
        layout.setSizeFull();
        return layout;
    }

    private VerticalLayout buildWorkflowTab() {
        TextField newStatusName = new TextField("Name");
        TextField newStatusDescription = new TextField("Description");
        Button add = new Button("Add", e -> addStatus(newStatusName, newStatusDescription));
        HorizontalLayout addForm = new HorizontalLayout(newStatusName, newStatusDescription, add);
        addForm.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        statusesGrid.addColumn(ProjectEntryStatusDto::sequence).setHeader("Order").setAutoWidth(true).setFlexGrow(0);
        statusesGrid.addColumn(ProjectEntryStatusDto::name).setHeader("Name");
        statusesGrid.addColumn(ProjectEntryStatusDto::description).setHeader("Description");
        statusesGrid.addColumn(ProjectEntryStatusDto::active).setHeader("Active").setAutoWidth(true).setFlexGrow(0);
        statusesGrid.addColumn(ProjectEntryStatusDto::startingStatus).setHeader("Starting").setAutoWidth(true).setFlexGrow(0);
        statusActionsColumn = statusesGrid.addComponentColumn(this::buildStatusActions).setHeader("").setFlexGrow(0);
        statusesGrid.setSizeFull();
        statusesGrid.getStyle().set("cursor", "pointer");
        statusesGrid.addItemClickListener(e -> {
            if (e.getColumn() != statusActionsColumn) {
                openEditStatusDialog(e.getItem());
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
            projectEntryStatusService.create(projectId,
                    new CreateProjectEntryStatusRequest(newStatusName.getValue(), newStatusDescription.getValue()),
                    currentUserId);
            newStatusName.clear();
            newStatusDescription.clear();
            refreshStatuses();
            Notification.show("Status added").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (IllegalArgumentException ex) {
            Notifications.showError(ex.getMessage());
        }
    }

    private HorizontalLayout buildStatusActions(ProjectEntryStatusDto status) {
        Button up = new Button(VaadinIcon.ARROW_UP.create());
        up.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        up.setTooltipText("Move up");
        up.addClickListener(e -> {
            projectEntryStatusService.moveUp(projectId, status.id(), currentUserId);
            refreshStatuses();
        });

        Button down = new Button(VaadinIcon.ARROW_DOWN.create());
        down.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        down.setTooltipText("Move down");
        down.addClickListener(e -> {
            projectEntryStatusService.moveDown(projectId, status.id(), currentUserId);
            refreshStatuses();
        });

        Button edit = new Button(VaadinIcon.EDIT.create());
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        edit.setTooltipText("Edit status");
        edit.addClickListener(e -> openEditStatusDialog(status));

        Button dependencies = new Button(VaadinIcon.CONNECT.create());
        dependencies.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        dependencies.setTooltipText("Manage depending statuses");
        dependencies.addClickListener(e -> openDependenciesDialog(status));

        Button delete = new Button(VaadinIcon.TRASH.create());
        delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        delete.setTooltipText("Delete status");
        delete.addClickListener(e -> confirmDeleteStatus(status));

        HorizontalLayout actions = new HorizontalLayout(up, down, edit, dependencies, delete);

        if (status.startingStatus()) {
            Icon starIcon = VaadinIcon.STAR.create();
            starIcon.setColor("var(--lumo-primary-color)");
            starIcon.setTooltipText("This is the starting status");
            actions.addComponentAsFirst(starIcon);
        } else {
            Button setStarting = new Button(VaadinIcon.STAR_O.create());
            setStarting.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
            setStarting.setTooltipText("Set as starting status");
            setStarting.addClickListener(e -> {
                projectEntryStatusService.setStarting(projectId, status.id(), currentUserId);
                refreshStatuses();
            });
            actions.addComponentAsFirst(setStarting);
        }

        return actions;
    }

    private void openEditStatusDialog(ProjectEntryStatusDto status) {
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
                projectEntryStatusService.update(projectId, status.id(), new UpdateProjectEntryStatusRequest(
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

    private void openDependenciesDialog(ProjectEntryStatusDto status) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Depending statuses for '" + status.name() + "'");

        Grid<ProjectEntryStatusDto> childrenGrid = new Grid<>(ProjectEntryStatusDto.class, false);
        ComboBox<ProjectEntryStatusDto> addChild = new ComboBox<>("Add depending status");
        addChild.setItemLabelGenerator(ProjectEntryStatusDto::name);

        childrenGrid.addColumn(ProjectEntryStatusDto::sequence).setHeader("Order").setAutoWidth(true).setFlexGrow(0);
        childrenGrid.addColumn(ProjectEntryStatusDto::name).setHeader("Name");
        childrenGrid.addComponentColumn(child -> {
            Button remove = new Button(VaadinIcon.TRASH.create());
            remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            remove.setTooltipText("Remove dependency");
            remove.addClickListener(e -> {
                projectEntryStatusService.removeChild(projectId, status.id(), child.id(), currentUserId);
                loadDependencies(status, childrenGrid, addChild);
            });
            return remove;
        }).setHeader("").setFlexGrow(0);
        childrenGrid.setWidthFull();
        childrenGrid.setHeight("250px");

        Button add = new Button("Add", e -> {
            ProjectEntryStatusDto selected = addChild.getValue();
            if (selected == null) {
                Notifications.showError("Select a status to add");
                return;
            }
            projectEntryStatusService.addChild(projectId, status.id(), selected.id(), currentUserId);
            loadDependencies(status, childrenGrid, addChild);
        });
        HorizontalLayout addForm = new HorizontalLayout(addChild, add);
        addForm.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        VerticalLayout content = new VerticalLayout(childrenGrid, addForm);
        content.setPadding(false);
        dialog.add(content);

        Button close = new Button("Close", e -> {
            dialog.close();
            refreshStatuses();
        });
        dialog.getFooter().add(close);

        loadDependencies(status, childrenGrid, addChild);
        dialog.open();
    }

    private void loadDependencies(ProjectEntryStatusDto status, Grid<ProjectEntryStatusDto> childrenGrid,
                                   ComboBox<ProjectEntryStatusDto> addChild) {
        List<ProjectEntryStatusDto> children = projectEntryStatusService.findChildren(projectId, status.id());
        childrenGrid.setItems(children);
        List<ProjectEntryStatusDto> allStatuses = projectEntryStatusService.findByProject(projectId);
        addChild.clear();
        addChild.setItems(allStatuses.stream()
                .filter(s -> !s.id().equals(status.id()))
                .filter(s -> children.stream().noneMatch(c -> c.id().equals(s.id())))
                .toList());
    }

    private void confirmDeleteStatus(ProjectEntryStatusDto status) {
        ConfirmDialog dialog = new ConfirmDialog(
                "Delete status",
                "Delete status '" + status.name() + "'? This can't be undone.",
                "Delete", e -> deleteStatus(status),
                "Cancel", e -> { }
        );
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    private void deleteStatus(ProjectEntryStatusDto status) {
        try {
            projectEntryStatusService.delete(projectId, status.id(), currentUserId);
            refreshStatuses();
            Notification.show("Status deleted").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (IllegalArgumentException ex) {
            Notifications.showError(ex.getMessage());
        }
    }

    private void refreshStatuses() {
        statusesGrid.setItems(projectEntryStatusService.findByProject(projectId));
    }
}
