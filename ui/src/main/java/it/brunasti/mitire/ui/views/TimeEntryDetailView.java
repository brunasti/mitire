package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.service.TimeEntryService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.TimeEntryDto;
import it.brunasti.mitire.backend.web.dto.UpdateTimeEntryRequest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Route(value = "time-entries", layout = MainLayout.class)
@PageTitle("Edit time entry | Mitire")
@PermitAll
public class TimeEntryDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final TimeEntryService timeEntryService;
    private final Long currentUserId;

    private final TextField project = new TextField("Project");
    private final TextField workDate = new TextField("Date");
    private final TextField status = new TextField("Status");
    private final NumberField hours = new NumberField("Hours");
    private final TextField description = new TextField("Description");

    private Long entryId;

    public TimeEntryDetailView(TimeEntryService timeEntryService, UserService userService,
                                AuthenticationContext authenticationContext) {
        this.timeEntryService = timeEntryService;
        this.currentUserId = authenticationContext.getPrincipalName()
                .map(userService::getByUsername)
                .map(u -> u.id())
                .orElseThrow();

        project.setReadOnly(true);
        workDate.setReadOnly(true);
        status.setReadOnly(true);
        hours.setStep(0.25);
        hours.setMin(0.25);
        hours.setMax(24);

        add(buildForm());
    }

    @Override
    public void setParameter(BeforeEvent event, Long entryId) {
        this.entryId = entryId;
        try {
            TimeEntryDto entry = timeEntryService.findByIdForUser(entryId, currentUserId);
            project.setValue(entry.projectCode());
            workDate.setValue(entry.workDate().toString());
            status.setValue(entry.status().name());
            hours.setValue(entry.hours().doubleValue());
            description.setValue(entry.description() != null ? entry.description() : "");
        } catch (NoSuchElementException | AccessDeniedException ex) {
            event.rerouteToError(NotFoundException.class, "Time entry not found");
        }
    }

    private FormLayout buildForm() {
        Button save = new Button("Save", e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> goBack());

        Button delete = new Button("Delete", e -> confirmDelete());
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        FormLayout form = new FormLayout(project, workDate, status, hours, description,
                new HorizontalLayout(save, cancel, delete));
        form.setMaxWidth("600px");
        return form;
    }

    private void save() {
        if (hours.getValue() == null) {
            Notification.show("Hours is required").addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        try {
            timeEntryService.update(entryId, currentUserId,
                    new UpdateTimeEntryRequest(BigDecimal.valueOf(hours.getValue()), description.getValue()));
            Notification.show("Time entry updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            goBack();
        } catch (IllegalArgumentException | AccessDeniedException ex) {
            Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void confirmDelete() {
        ConfirmDialog dialog = new ConfirmDialog(
                "Delete time entry",
                "Are you sure you want to delete this time entry? This cannot be undone.",
                "Delete", e -> delete(),
                "Cancel", e -> { }
        );
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    private void delete() {
        try {
            timeEntryService.delete(entryId, currentUserId);
            Notification.show("Time entry deleted").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            goBack();
        } catch (AccessDeniedException ex) {
            Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void goBack() {
        UI.getCurrent().navigate(TimeEntriesView.class);
    }
}
