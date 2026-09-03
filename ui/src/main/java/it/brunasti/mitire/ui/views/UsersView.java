package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.service.GroupService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.CreateUserRequest;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.brunasti.mitire.ui.util.Notifications;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Users | MiTiRe")
@RolesAllowed("ADMIN")
public class UsersView extends VerticalLayout {

    private final UserService userService;
    private final Grid<UserDto> grid = new Grid<>(UserDto.class, false);

    public UsersView(UserService userService, GroupService groupService) {
        this.userService = userService;

        setSizeFull();

        H2 title = new H2("Users");

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Users", buildGrid());
        tabSheet.add("Add User", buildForm(groupService));
        tabSheet.setSizeFull();

        add(title, tabSheet);
        setFlexGrow(1, tabSheet);

        refreshGrid();
    }

    private FormLayout buildForm(GroupService groupService) {
        TextField username = new TextField("Username");
        TextField fullName = new TextField("Full name");
        TextField email = new TextField("Email");
        PasswordField password = new PasswordField("Password");
        ComboBox<Role> role = new ComboBox<>("Role");
        role.setItems(Role.values());
        MultiSelectComboBox<GroupDto> groups = new MultiSelectComboBox<>("Groups");
        groups.setItems(groupService.findAll());
        groups.setItemLabelGenerator(GroupDto::name);

        Button submit = new Button("Create", e -> {
            if (username.getValue().isBlank() || fullName.getValue().isBlank()
                    || email.getValue().isBlank() || password.getValue().isBlank() || role.getValue() == null) {
                Notifications.showError("Username, full name, email, password and role are required");
                return;
            }
            try {
                var groupIds = groups.getSelectedItems().stream().map(GroupDto::id).toList();
                userService.create(new CreateUserRequest(username.getValue(), fullName.getValue(), email.getValue(),
                        password.getValue(), role.getValue(), groupIds));
                Notification.show("User created").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                username.clear();
                fullName.clear();
                email.clear();
                password.clear();
                role.clear();
                groups.clear();
                refreshGrid();
            } catch (IllegalArgumentException ex) {
                Notifications.showError(ex.getMessage());
            }
        });

        FormLayout form = new FormLayout(username, fullName, email, password, role, groups, submit);
        form.setMaxWidth("600px");
        return form;
    }

    private Grid<UserDto> buildGrid() {
        grid.addColumn(UserDto::username).setHeader("Username").setSortable(true);
        grid.addColumn(UserDto::fullName).setHeader("Full name").setSortable(true);
        grid.addColumn(UserDto::email).setHeader("Email").setSortable(true);
        grid.addColumn(UserDto::role).setHeader("Role").setSortable(true).setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(u -> u.groups().stream().map(GroupDto::name).reduce((a, b) -> a + ", " + b).orElse("-"))
                .setHeader("Groups").setSortable(true);
        grid.addColumn(UserDto::enabled).setHeader("Enabled").setSortable(true).setAutoWidth(true).setFlexGrow(0);
        grid.setSizeFull();
        grid.getStyle().set("cursor", "pointer");
        grid.addItemClickListener(e -> UI.getCurrent().navigate(UserDetailView.class, e.getItem().id()));
        return grid;
    }

    private void refreshGrid() {
        grid.setItems(userService.findAll());
    }
}
