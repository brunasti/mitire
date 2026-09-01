package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

@PermitAll
public class MainLayout extends AppLayout {

    public MainLayout(AuthenticationContext authenticationContext, UserService userService) {
        Image logoIcon = new Image("mitire-icon.png", "MiTiRe logo");
        logoIcon.setHeight("32px");
        logoIcon.setWidth("32px");

        Button logo = new Button(logoIcon, e -> UI.getCurrent().navigate(""));
        logo.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        logo.setTooltipText("Home");

        H1 title = new H1("MiTiRe");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);

        String fullName = authenticationContext.getAuthenticatedUser(org.springframework.security.core.userdetails.User.class)
                .map(user -> userService.getByUsername(user.getUsername()).fullName())
                .orElse("");
        Span userName = new Span(fullName);
        userName.getStyle().set("margin-right", "0.25rem");

        Button profile = new Button(VaadinIcon.USER.create(), e -> UI.getCurrent().navigate(ProfileView.class));
        profile.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        profile.setTooltipText("My profile");

        Button logout = new Button("Log out", e -> authenticationContext.logout());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout header = new HorizontalLayout(logo, new DrawerToggle(), title, userName, profile, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.expand(title);
        addToNavbar(header);

        VerticalLayout nav = new VerticalLayout(
                new RouterLink("Time entries", TimeEntriesView.class)
        );
        if (isAdmin()) {
            nav.add(
                    new RouterLink("Projects", ProjectsView.class),
                    new RouterLink("Groups", GroupsView.class),
                    new RouterLink("Users", UsersView.class)
            );
        }
        nav.setPadding(true);
        addToDrawer(nav);
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
