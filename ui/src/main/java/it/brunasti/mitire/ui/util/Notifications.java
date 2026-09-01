package it.brunasti.mitire.ui.util;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;

/**
 * Shared error/warning popup, styled with a solid orange background so it can't be
 * mistaken for a routine (success) toast, regardless of the current Lumo theme.
 */
public final class Notifications {

    private Notifications() {
    }

    public static void showError(String message) {
        Div content = new Div();
        content.setText(message);
        content.getStyle()
                .set("background-color", "#e65100")
                .set("color", "white")
                .set("font-size", "1.05rem")
                .set("font-weight", "600")
                .set("padding", "0.75rem 1.25rem")
                .set("border-radius", "var(--lumo-border-radius-m)");

        Notification notification = new Notification(content);
        notification.setPosition(Notification.Position.TOP_CENTER);
        notification.setDuration(6000);
        notification.open();
    }
}
