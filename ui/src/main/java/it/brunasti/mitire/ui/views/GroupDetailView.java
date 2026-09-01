package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.service.GroupService;
import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.UpdateGroupRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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

import java.util.HashSet;
import java.util.NoSuchElementException;

@Route(value = "groups", layout = MainLayout.class)
@PageTitle("Group details | Mitire")
@RolesAllowed("ADMIN")
public class GroupDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final GroupService groupService;
    private final UserService userService;

    private final TextField name = new TextField("Name");
    private final MultiSelectComboBox<ProjectDto> projects = new MultiSelectComboBox<>("Accessible projects");
    private final Grid<UserDto> usersGrid = new Grid<>(UserDto.class, false);

    private Long groupId;

    public GroupDetailView(GroupService groupService, ProjectService projectService, UserService userService) {
        this.groupService = groupService;
        this.userService = userService;

        setSizeFull();

        projects.setItems(projectService.findAll());
        projects.setItemLabelGenerator(p -> p.code() + " - " + p.name());

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
        try {
            GroupDto group = groupService.findById(groupId);
            name.setValue(group.name());
            projects.setValue(new HashSet<>(group.projects()));
        } catch (NoSuchElementException ex) {
            event.rerouteToError(NotFoundException.class, "Group not found");
            return;
        }
        usersGrid.setItems(userService.findByGroup(groupId));
    }

    private FormLayout buildDetailsTab() {
        Button save = new Button("Save", e -> save());
        FormLayout form = new FormLayout(name, save);
        form.setMaxWidth("600px");
        return form;
    }

    private FormLayout buildProjectsTab() {
        Button save = new Button("Save", e -> save());
        FormLayout form = new FormLayout(projects, save);
        form.setMaxWidth("600px");
        return form;
    }

    private VerticalLayout buildUsersTab() {
        usersGrid.addColumn(UserDto::username).setHeader("Username").setSortable(true);
        usersGrid.addColumn(UserDto::fullName).setHeader("Full name");
        usersGrid.addColumn(UserDto::email).setHeader("Email");
        usersGrid.addColumn(UserDto::role).setHeader("Role");
        usersGrid.addColumn(UserDto::enabled).setHeader("Enabled");
        usersGrid.setSizeFull();

        VerticalLayout layout = new VerticalLayout(usersGrid);
        layout.setSizeFull();
        return layout;
    }

    private void save() {
        if (name.getValue().isBlank()) {
            Notification.show("Name is required").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        var projectIds = projects.getSelectedItems().stream().map(ProjectDto::id).toList();
        try {
            groupService.update(groupId, new UpdateGroupRequest(name.getValue(), projectIds));
            Notification.show("Group updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
