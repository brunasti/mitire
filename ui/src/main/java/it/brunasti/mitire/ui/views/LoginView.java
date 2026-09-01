package it.brunasti.mitire.ui.views;

import com.vaadin.flow.component.login.LoginOverlay;
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
        setTitle("MiTiRe");
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
