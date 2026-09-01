package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.service.GroupService;
import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.UpdateGroupRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
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
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.NoSuchElementException;

@Route(value = "groups", layout = MainLayout.class)
@PageTitle("Group details | Mitire")
@RolesAllowed("ADMIN")
public class GroupDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final GroupService groupService;
    private final UserService userService;

    private final TextField name = new TextField("Name");
    private final ComboBox<ProjectDto> addProject = new ComboBox<>("Add project");
    private final ComboBox<UserDto> addUser = new ComboBox<>("Add user");
    private final Grid<ProjectDto> projectsGrid = new Grid<>(ProjectDto.class, false);
    private final Grid<UserDto> usersGrid = new Grid<>(UserDto.class, false);

    private List<ProjectDto> allProjects;
    private List<UserDto> allUsers;
    private Long groupId;
    private List<Long> currentProjectIds = List.of();

    public GroupDetailView(GroupService groupService, ProjectService projectService, UserService userService) {
        this.groupService = groupService;
        this.userService = userService;

        setSizeFull();

        allProjects = projectService.findAll();
        addProject.setItemLabelGenerator(p -> p.code() + " - " + p.name());

        allUsers = userService.findAll();
        addUser.setItemLabelGenerator(UserDto::username);

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Group details", buildDetailsTab());
        tabSheet.add("Projects", buildProjectsTab());
        tabSheet.add("Users", buildUsersTab());
        tabSheet.setSizeFull();

        add(new RouterLink("← Back to groups", GroupsView.class), tabSheet);
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
        currentProjectIds = group.projects().stream().map(ProjectDto::id).toList();
        projectsGrid.setItems(group.projects());
        addProject.setItems(allProjects.stream().filter(p -> !group.projects().contains(p)).toList());

        List<UserDto> members = userService.findByGroup(groupId);
        usersGrid.setItems(members);
        addUser.setItems(allUsers.stream().filter(u -> members.stream().noneMatch(m -> m.id().equals(u.id()))).toList());
    }

    private FormLayout buildDetailsTab() {
        Button save = new Button("Save", e -> save());
        FormLayout form = new FormLayout(name, save);
        form.setMaxWidth("600px");
        return form;
    }

    private VerticalLayout buildProjectsTab() {
        Button add = new Button("Add", e -> addProjectLink());
        HorizontalLayout addForm = new HorizontalLayout(addProject, add);
        addForm.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        projectsGrid.addColumn(ProjectDto::code).setHeader("Code").setSortable(true);
        projectsGrid.addColumn(ProjectDto::name).setHeader("Name");
        projectsGrid.addColumn(ProjectDto::active).setHeader("Active");
        projectsGrid.setSizeFull();
        projectsGrid.getStyle().set("cursor", "pointer");
        projectsGrid.addItemClickListener(e -> UI.getCurrent().navigate(ProjectDetailView.class, e.getItem().id()));

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
        usersGrid.addColumn(UserDto::role).setHeader("Role");
        usersGrid.addColumn(UserDto::enabled).setHeader("Enabled");
        usersGrid.setSizeFull();
        usersGrid.getStyle().set("cursor", "pointer");
        usersGrid.addItemClickListener(e -> UI.getCurrent().navigate(UserDetailView.class, e.getItem().id()));

        VerticalLayout layout = new VerticalLayout(addForm, usersGrid);
        layout.setSizeFull();
        return layout;
    }

    private void addProjectLink() {
        ProjectDto selected = addProject.getValue();
        if (selected == null) {
            Notification.show("Select a project to add").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        GroupDto updated = groupService.addProject(groupId, selected.id());
        currentProjectIds = updated.projects().stream().map(ProjectDto::id).toList();
        projectsGrid.setItems(updated.projects());
        addProject.setItems(allProjects.stream().filter(p -> !updated.projects().contains(p)).toList());
        addProject.clear();
        Notification.show("Project added").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void addUserLink() {
        UserDto selected = addUser.getValue();
        if (selected == null) {
            Notification.show("Select a user to add").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        userService.addToGroup(selected.id(), groupId);
        List<UserDto> members = userService.findByGroup(groupId);
        usersGrid.setItems(members);
        addUser.setItems(allUsers.stream().filter(u -> members.stream().noneMatch(m -> m.id().equals(u.id()))).toList());
        addUser.clear();
        Notification.show("User added").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void save() {
        if (name.getValue().isBlank()) {
            Notification.show("Name is required").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        try {
            groupService.update(groupId, new UpdateGroupRequest(name.getValue(), currentProjectIds));
            Notification.show("Group updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
