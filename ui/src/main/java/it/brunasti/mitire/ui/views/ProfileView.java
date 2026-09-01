package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.ChangeOwnPasswordRequest;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.UpdateOwnProfileRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("My Profile | Mitire")
@PermitAll
public class ProfileView extends VerticalLayout {

    private final UserService userService;
    private final String username;

    private final TextField usernameField = new TextField("Username");
    private final TextField fullName = new TextField("Full name");
    private final TextField email = new TextField("Email");
    private final TextField role = new TextField("Role");
    private final TextField groups = new TextField("Groups");
    private final Checkbox enabled = new Checkbox("Enabled");

    private final PasswordField currentPassword = new PasswordField("Current password");
    private final PasswordField newPassword = new PasswordField("New password");
    private final PasswordField confirmPassword = new PasswordField("Confirm new password");

    public ProfileView(UserService userService, AuthenticationContext authenticationContext) {
        this.userService = userService;
        this.username = authenticationContext.getPrincipalName().orElseThrow();

        UserDto user = userService.getByUsername(username);
        loadInto(user);

        add(new H2("My Profile"), buildProfileForm(), new H2("Change Password"), buildPasswordForm());
    }

    private void loadInto(UserDto user) {
        usernameField.setValue(user.username());
        usernameField.setReadOnly(true);
        fullName.setValue(user.fullName());
        email.setValue(user.email());
        role.setValue(user.role().name());
        role.setReadOnly(true);
        role.setHelperText("Only an administrator can change your role.");
        groups.setValue(user.groups().stream().map(GroupDto::name).reduce((a, b) -> a + ", " + b).orElse("-"));
        groups.setReadOnly(true);
        enabled.setValue(user.enabled());
        enabled.setReadOnly(true);
    }

    private FormLayout buildProfileForm() {
        Button save = new Button("Save profile", e -> saveProfile());
        FormLayout form = new FormLayout(usernameField, fullName, email, role, groups, enabled, save);
        form.setMaxWidth("600px");
        return form;
    }

    private FormLayout buildPasswordForm() {
        Button change = new Button("Change password", e -> changePassword());
        FormLayout form = new FormLayout(currentPassword, newPassword, confirmPassword, change);
        form.setMaxWidth("600px");
        return form;
    }

    private void saveProfile() {
        if (fullName.getValue().isBlank() || email.getValue().isBlank()) {
            Notification.show("Full name and email are required").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        try {
            userService.updateOwnProfile(username, new UpdateOwnProfileRequest(fullName.getValue(), email.getValue()));
            Notification.show("Profile updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void changePassword() {
        if (currentPassword.getValue().isBlank() || newPassword.getValue().isBlank()) {
            Notification.show("Current and new password are required").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        if (!newPassword.getValue().equals(confirmPassword.getValue())) {
            Notification.show("New password and confirmation do not match").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        try {
            userService.changeOwnPassword(username, currentPassword.getValue(), newPassword.getValue());
            currentPassword.clear();
            newPassword.clear();
            confirmPassword.clear();
            Notification.show("Password changed").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
