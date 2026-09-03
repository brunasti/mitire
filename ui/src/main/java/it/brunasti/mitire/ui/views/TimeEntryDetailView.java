package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.service.ProjectEntityStatusService;
import it.brunasti.mitire.backend.service.TimeEntryService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.ProjectEntityStatusDto;
import it.brunasti.mitire.backend.web.dto.TimeEntryDto;
import it.brunasti.mitire.backend.web.dto.UpdateTimeEntryRequest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
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
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import it.brunasti.mitire.ui.util.Notifications;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

@Route(value = "time-entries", layout = MainLayout.class)
@PageTitle("Edit time entry | MiTiRe")
@PermitAll
public class TimeEntryDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final TimeEntryService timeEntryService;
    private final ProjectEntityStatusService projectEntityStatusService;
    private final UserService userService;
    private final Long currentUserId;
    private final Role currentUserRole;

    private final Div projectField = new Div();
    private final RouterLink userLink = new RouterLink();
    private final TextField workDate = new TextField("Date");
    private final Div statusField = new Div();
    private final NumberField hours = new NumberField("Hours");
    private final TextField description = new TextField("Description");

    private ComboBox<ProjectEntityStatusDto> statusComboBox;
    private Long entryId;

    public TimeEntryDetailView(TimeEntryService timeEntryService, ProjectEntityStatusService projectEntityStatusService,
                                UserService userService, AuthenticationContext authenticationContext) {
        this.timeEntryService = timeEntryService;
        this.projectEntityStatusService = projectEntityStatusService;
        var currentUser = authenticationContext.getPrincipalName()
                .map(userService::getByUsername)
                .orElseThrow();
        this.currentUserId = currentUser.id();
        this.currentUserRole = currentUser.role();
        this.userService = userService;

        workDate.setReadOnly(true);
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
            projectField.removeAll();
            if (currentUserRole == Role.ADMIN) {
                projectField.add(new RouterLink(entry.projectCode(), ProjectDetailView.class, entry.projectId()));
            } else {
                projectField.add(new Span(entry.projectCode()));
            }
            userLink.setText(entry.userFullName());
            if (entry.userId().equals(currentUserId)) {
                userLink.setRoute(ProfileView.class);
            } else {
                userLink.setRoute(UserDetailView.class, entry.userId());
            }
            workDate.setValue(entry.workDate().toString());
            hours.setValue(entry.hours().doubleValue());
            description.setValue(entry.description() != null ? entry.description() : "");

            boolean isProjectAdmin = userService.effectiveRole(currentUserId, entry.projectId()) == Role.ADMIN;

            statusField.removeAll();
            if (isProjectAdmin) {
                ProjectEntityStatusDto currentStatus = projectEntityStatusService.findById(entry.projectId(), entry.statusId());
                List<ProjectEntityStatusDto> children = projectEntityStatusService.findChildren(entry.projectId(), entry.statusId());
                List<ProjectEntityStatusDto> selectable = Stream.concat(Stream.of(currentStatus), children.stream())
                        .filter(s -> s.active() || s.id().equals(entry.statusId()))
                        .distinct()
                        .toList();
                statusComboBox = new ComboBox<>();
                statusComboBox.setItems(selectable);
                statusComboBox.setItemLabelGenerator(ProjectEntityStatusDto::name);
                selectable.stream().filter(s -> s.id().equals(entry.statusId())).findFirst()
                        .ifPresent(statusComboBox::setValue);
                statusField.add(statusComboBox);
            } else {
                statusComboBox = null;
                statusField.add(new Span(entry.statusName()));
            }
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

        FormLayout form = new FormLayout();
        form.addFormItem(projectField, "Project");
        form.addFormItem(userLink, "User");
        form.addFormItem(statusField, "Status");
        form.add(workDate, hours, description);
        form.add(new HorizontalLayout(save, cancel, delete));
        form.setMaxWidth("600px");
        return form;
    }

    private void save() {
        if (hours.getValue() == null) {
            Notifications.showError("Hours is required");
            return;
        }
        Long statusId = statusComboBox != null && statusComboBox.getValue() != null
                ? statusComboBox.getValue().id()
                : null;
        try {
            timeEntryService.update(entryId, currentUserId,
                    new UpdateTimeEntryRequest(BigDecimal.valueOf(hours.getValue()), description.getValue(), statusId));
            Notification.show("Time entry updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            goBack();
        } catch (IllegalArgumentException | AccessDeniedException ex) {
            Notifications.showError(ex.getMessage());
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
            Notifications.showError(ex.getMessage());
        }
    }

    private void goBack() {
        UI.getCurrent().navigate(TimeEntriesView.class);
    }
}
