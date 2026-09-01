package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.service.GroupService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.CreateUserRequest;
import it.brunasti.mitire.backend.web.dto.GroupDto;
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
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Users | Mitire")
@RolesAllowed("ADMIN")
public class UsersView extends VerticalLayout {

    private final UserService userService;
    private final Grid<UserDto> grid = new Grid<>(UserDto.class, false);

    public UsersView(UserService userService, GroupService groupService) {
        this.userService = userService;

        add(buildForm(groupService));
        add(buildGrid());

        refreshGrid();
    }

    private FormLayout buildForm(GroupService groupService) {
        TextField username = new TextField("Username");
        TextField fullName = new TextField("Full name");
        TextField email = new TextField("Email");
        PasswordField password = new PasswordField("Password");
        ComboBox<Role> role = new ComboBox<>("Role");
        role.setItems(Role.values());
        ComboBox<GroupDto> group = new ComboBox<>("Group");
        group.setItems(groupService.findAll());
        group.setItemLabelGenerator(GroupDto::name);
        group.setClearButtonVisible(true);

        Button submit = new Button("Create", e -> {
            if (username.getValue().isBlank() || fullName.getValue().isBlank()
                    || email.getValue().isBlank() || password.getValue().isBlank() || role.getValue() == null) {
                Notification.show("Username, full name, email, password and role are required")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                Long groupId = group.getValue() != null ? group.getValue().id() : null;
                userService.create(new CreateUserRequest(username.getValue(), fullName.getValue(), email.getValue(),
                        password.getValue(), role.getValue(), groupId));
                Notification.show("User created").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                username.clear();
                fullName.clear();
                email.clear();
                password.clear();
                role.clear();
                group.clear();
                refreshGrid();
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        return new FormLayout(username, fullName, email, password, role, group, submit);
    }

    private Grid<UserDto> buildGrid() {
        grid.addColumn(UserDto::username).setHeader("Username").setSortable(true);
        grid.addColumn(UserDto::fullName).setHeader("Full name");
        grid.addColumn(UserDto::email).setHeader("Email");
        grid.addColumn(UserDto::role).setHeader("Role");
        grid.addColumn(u -> u.groupName() != null ? u.groupName() : "-").setHeader("Group");
        grid.addColumn(UserDto::enabled).setHeader("Enabled");
        grid.setWidthFull();
        grid.setHeight("400px");
        grid.getStyle().set("cursor", "pointer");
        grid.addItemClickListener(e -> UI.getCurrent().navigate(UserDetailView.class, e.getItem().id()));
        return grid;
    }

    private void refreshGrid() {
        grid.setItems(userService.findAll());
    }
}
