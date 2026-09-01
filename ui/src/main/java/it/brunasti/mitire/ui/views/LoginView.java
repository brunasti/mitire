package it.brunasti.mitire.ui.views;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Log in | MiTiRe")
@AnonymousAllowed
public class LoginView extends LoginOverlay implements BeforeEnterObserver {

    public LoginView() {
        setAction("login");
        setOpened(true);

        Image logo = new Image("mitire-icon.png", "MiTiRe logo");
        logo.setHeight("32px");
        logo.setWidth("32px");
        HorizontalLayout title = new HorizontalLayout(logo, new Span("MiTiRe"));
        title.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        setTitle(title);

        setDescription("Team time reporting");
        setForgotPasswordButtonVisible(false);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            setError(true);
        }
    }
}
