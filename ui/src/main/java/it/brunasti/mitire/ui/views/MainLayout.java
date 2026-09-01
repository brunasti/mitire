package it.brunasti.mitire.ui.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class MainLayout extends AppLayout {

    public MainLayout(AuthenticationContext authenticationContext) {
        H1 title = new H1("Mitire");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);

        Button logout = new Button("Log out", e -> authenticationContext.logout());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.expand(title);
        addToNavbar(header);

        VerticalLayout nav = new VerticalLayout(
                new RouterLink("Time entries", TimeEntriesView.class),
                new RouterLink("Projects", ProjectsView.class)
        );
        nav.setPadding(true);
        addToDrawer(nav);
    }
}
