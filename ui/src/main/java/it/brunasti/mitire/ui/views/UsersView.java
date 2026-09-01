package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.service.GroupService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.CreateUserRequest;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.UpdateUserRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Users | Mitire")
@RolesAllowed("ADMIN")
public class UsersView extends VerticalLayout {

    private final UserService userService;
    private final Grid<UserDto> grid = new Grid<>(UserDto.class, false);

    private final TextField username = new TextField("Username");
    private final TextField fullName = new TextField("Full name");
    private final TextField email = new TextField("Email");
    private final PasswordField password = new PasswordField("Password");
    private final ComboBox<Role> role = new ComboBox<>("Role");
    private final ComboBox<GroupDto> group = new ComboBox<>("Group");
    private final Checkbox enabled = new Checkbox("Enabled", true);
    private final Button submit = new Button("Create");
    private final Button cancel = new Button("Cancel");

    private List<GroupDto> groups;
    private Long editingId;
    private Role editingOriginalRole;

    public UsersView(UserService userService, GroupService groupService) {
        this.userService = userService;

        password.setHelperText("Required for new users. When editing, leave blank to keep the current password.");

        role.setItems(Role.values());

        groups = groupService.findAll();
        group.setItems(groups);
        group.setItemLabelGenerator(GroupDto::name);
        group.setClearButtonVisible(true);

        cancel.setVisible(false);
        submit.addClickListener(e -> save());
        cancel.addClickListener(e -> resetForm());

        FormLayout form = new FormLayout(username, fullName, email, password, role, group, enabled,
                new HorizontalLayout(submit, cancel));
        add(form);
        add(buildGrid());

        refreshGrid();
    }

    private void save() {
        if (username.getValue().isBlank() || fullName.getValue().isBlank()
                || email.getValue().isBlank() || role.getValue() == null) {
            Notification.show("Username, full name, email and role are required").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        Long groupId = group.getValue() != null ? group.getValue().id() : null;
        try {
            if (editingId == null) {
                if (password.getValue().isBlank()) {
                    Notification.show("Password is required for a new user").addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                userService.create(new CreateUserRequest(username.getValue(), fullName.getValue(), email.getValue(),
                        password.getValue(), role.getValue(), groupId));
                Notification.show("User created").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                if (!password.getValue().isBlank() && editingOriginalRole != Role.ADMIN) {
                    userService.updatePassword(editingId, password.getValue());
                }
                userService.update(editingId, new UpdateUserRequest(fullName.getValue(), email.getValue(),
                        role.getValue(), groupId, enabled.getValue()));
                Notification.show("User updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            resetForm();
            refreshGrid();
        } catch (IllegalArgumentException | AccessDeniedException ex) {
            Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void resetForm() {
        editingId = null;
        editingOriginalRole = null;
        username.clear();
        username.setEnabled(true);
        fullName.clear();
        email.clear();
        password.clear();
        password.setEnabled(true);
        password.setHelperText("Required for new users. When editing, leave blank to keep the current password.");
        role.clear();
        group.clear();
        enabled.setValue(true);
        submit.setText("Create");
        cancel.setVisible(false);
    }

    private void edit(UserDto user) {
        editingId = user.id();
        editingOriginalRole = user.role();
        username.setValue(user.username());
        username.setEnabled(false);
        fullName.setValue(user.fullName());
        email.setValue(user.email());
        password.clear();
        password.setEnabled(user.role() != Role.ADMIN);
        password.setHelperText(user.role() == Role.ADMIN
                ? "An ADMIN user's password can't be changed here."
                : "Leave blank to keep the current password.");
        role.setValue(user.role());
        group.setValue(groups.stream().filter(g -> g.id().equals(user.groupId())).findFirst().orElse(null));
        enabled.setValue(user.enabled());
        submit.setText("Update");
        cancel.setVisible(true);
    }

    private Grid<UserDto> buildGrid() {
        grid.addColumn(UserDto::username).setHeader("Username").setSortable(true);
        grid.addColumn(UserDto::fullName).setHeader("Full name");
        grid.addColumn(UserDto::email).setHeader("Email");
        grid.addColumn(UserDto::role).setHeader("Role");
        grid.addColumn(u -> u.groupName() != null ? u.groupName() : "-").setHeader("Group");
        grid.addColumn(UserDto::enabled).setHeader("Enabled");
        grid.addComponentColumn(u -> new Button("Edit", e -> edit(u))).setHeader("");
        grid.setWidthFull();
        grid.setHeight("400px");
        return grid;
    }

    private void refreshGrid() {
        grid.setItems(userService.findAll());
    }
}
