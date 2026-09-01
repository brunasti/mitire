package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.service.GroupService;
import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.service.TimeEntryService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.TimeEntryDto;
import it.brunasti.mitire.backend.web.dto.UpdateProjectRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
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

    private final TextField code = new TextField("Code");
    private final TextField name = new TextField("Name");
    private final Checkbox active = new Checkbox("Active");
    private final DatePicker startDate = new DatePicker("Start date");
    private final DatePicker endDate = new DatePicker("End date");
    private final ComboBox<GroupDto> addGroup = new ComboBox<>("Add group");
    private final Span projectNameLabel = new Span();

    private final Grid<TimeEntryDto> entriesGrid = new Grid<>(TimeEntryDto.class, false);
    private final Grid<UserDto> usersGrid = new Grid<>(UserDto.class, false);
    private final Grid<GroupDto> groupsGrid = new Grid<>(GroupDto.class, false);

    private Grid.Column<GroupDto> groupActionsColumn;

    private List<GroupDto> allGroups;
    private Long projectId;

    public ProjectDetailView(ProjectService projectService, TimeEntryService timeEntryService,
                              UserService userService, GroupService groupService) {
        this.projectService = projectService;
        this.timeEntryService = timeEntryService;
        this.userService = userService;
        this.groupService = groupService;

        setSizeFull();

        startDate.setWidth("160px");
        endDate.setWidth("160px");

        allGroups = groupService.findAll();
        addGroup.setItemLabelGenerator(GroupDto::name);

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Project details", buildDetailsTab());
        tabSheet.add("Time Entries", buildEntriesTab());
        tabSheet.add("Users", buildUsersTab());
        tabSheet.add("Groups", buildGroupsTab());
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
        try {
            ProjectDto project = projectService.findById(projectId);
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
        usersGrid.setItems(userService.findByProjectAccess(projectId));
        refreshGroups();
    }

    private FormLayout buildDetailsTab() {
        code.setReadOnly(true);

        Button save = new Button("Save", e -> {
            try {
                ProjectDto updated = projectService.update(projectId, new UpdateProjectRequest(
                        name.getValue(), active.getValue(), startDate.getValue(), endDate.getValue()));
                projectNameLabel.setText(updated.name());
                Notification.show("Project updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                Notifications.showError(ex.getMessage());
            }
        });

        HorizontalLayout startAndEndDate = new HorizontalLayout(startDate, endDate);

        FormLayout form = new FormLayout(code, name, active, startAndEndDate, save);
        form.setMaxWidth("600px");
        return form;
    }

    private VerticalLayout buildEntriesTab() {
        entriesGrid.addColumn(TimeEntryDto::workDate).setHeader("Date").setSortable(true);
        entriesGrid.addColumn(TimeEntryDto::username).setHeader("User").setSortable(true);
        entriesGrid.addColumn(TimeEntryDto::hours).setHeader("Hours");
        entriesGrid.addColumn(TimeEntryDto::description).setHeader("Description");
        entriesGrid.addColumn(TimeEntryDto::status).setHeader("Status");
        entriesGrid.setSizeFull();

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
        usersGrid.setItems(userService.findByProjectAccess(projectId));
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
        usersGrid.setItems(userService.findByProjectAccess(projectId));
        Notification.show("Group removed from project").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void refreshGroups() {
        List<GroupDto> linkedGroups = groupService.findByProject(projectId);
        groupsGrid.setItems(linkedGroups);
        addGroup.clear();
        addGroup.setItems(allGroups.stream().filter(g -> !linkedGroups.contains(g)).toList());
    }
}
