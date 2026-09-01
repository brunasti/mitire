package it.brunasti.mitire.ui.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Replaces Vaadin's default "Could not navigate to '...'" page shown when a
 * logged-in user's role doesn't grant access to a route.
 */
@ParentLayout(MainLayout.class)
@PageTitle("Access denied | MiTiRe")
@AnonymousAllowed
public class AccessDeniedView extends VerticalLayout implements HasErrorParameter<AccessDeniedException> {

    public AccessDeniedView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        Icon icon = VaadinIcon.LOCK.create();
        icon.setSize("64px");
        icon.addClassNames(LumoUtility.TextColor.ERROR);

        H2 title = new H2("Access denied");

        Paragraph message = new Paragraph(
                "You don't have permission to view this page. If you think this is a "
                        + "mistake, contact your administrator.");
        message.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.TextAlignment.CENTER);

        Button home = new Button("Go to home page", e -> UI.getCurrent().navigate(""));
        home.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout content = new VerticalLayout(icon, title, message, home);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setMaxWidth("420px");

        add(content);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<AccessDeniedException> parameter) {
        return HttpStatusCode.FORBIDDEN.getCode();
    }
}
