package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.service.ProjectEntryStatusService;
import it.brunasti.mitire.backend.service.TimeEntryService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.CreateTimeEntryNoteRequest;
import it.brunasti.mitire.backend.web.dto.ProjectEntryStatusDto;
import it.brunasti.mitire.backend.web.dto.TimeEntryDto;
import it.brunasti.mitire.backend.web.dto.TimeEntryNoteDto;
import it.brunasti.mitire.backend.web.dto.TimeEntryTransitionDto;
import it.brunasti.mitire.backend.web.dto.UpdateTimeEntryRequest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import it.brunasti.mitire.ui.util.Formatters;
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
    private final ProjectEntryStatusService projectEntryStatusService;
    private final UserService userService;
    private final Long currentUserId;
    private final Role currentUserRole;

    private final Div projectField = new Div();
    private final RouterLink userLink = new RouterLink();
    private final TextField workDate = new TextField("Date");
    private final TextField createdAt = new TextField("Created");
    private final Div statusField = new Div();
    private final NumberField hours = new NumberField("Hours");
    private final TextArea description = new TextArea("Description");

    private final Grid<TimeEntryTransitionDto> transitionsGrid = new Grid<>(TimeEntryTransitionDto.class, false);

    private final VerticalLayout notesContainer = new VerticalLayout();
    private final TextArea newNoteText = new TextArea("New note");

    private ComboBox<ProjectEntryStatusDto> statusComboBox;
    private Long entryId;

    public TimeEntryDetailView(TimeEntryService timeEntryService, ProjectEntryStatusService projectEntryStatusService,
                                UserService userService, AuthenticationContext authenticationContext) {
        this.timeEntryService = timeEntryService;
        this.projectEntryStatusService = projectEntryStatusService;
        var currentUser = authenticationContext.getPrincipalName()
                .map(userService::getByUsername)
                .orElseThrow();
        this.currentUserId = currentUser.id();
        this.currentUserRole = currentUser.role();
        this.userService = userService;

        workDate.setReadOnly(true);
        createdAt.setReadOnly(true);
        hours.setStep(0.25);
        hours.setMin(0.25);
        hours.setMax(24);
        description.setHeight("150px");
        newNoteText.setHeight("100px");
        newNoteText.setWidth("600px");

        add(buildForm(), buildTransitionsSection(), buildNotesSection());
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
            createdAt.setValue(Formatters.timestamp(entry.createdAt()));
            hours.setValue(entry.hours().doubleValue());
            description.setValue(entry.description() != null ? entry.description() : "");

            boolean isProjectAdmin = userService.effectiveRole(currentUserId, entry.projectId()) == Role.ADMIN;

            statusField.removeAll();
            if (isProjectAdmin) {
                ProjectEntryStatusDto currentStatus = projectEntryStatusService.findById(entry.projectId(), entry.statusId());
                List<ProjectEntryStatusDto> children = projectEntryStatusService.findChildren(entry.projectId(), entry.statusId());
                List<ProjectEntryStatusDto> selectable = Stream.concat(Stream.of(currentStatus), children.stream())
                        .filter(s -> s.active() || s.id().equals(entry.statusId()))
                        .distinct()
                        .toList();
                statusComboBox = new ComboBox<>();
                statusComboBox.setItems(selectable);
                statusComboBox.setItemLabelGenerator(ProjectEntryStatusDto::name);
                selectable.stream().filter(s -> s.id().equals(entry.statusId())).findFirst()
                        .ifPresent(statusComboBox::setValue);
                statusField.add(statusComboBox);
            } else {
                statusComboBox = null;
                statusField.add(new Span(entry.statusName()));
            }
            transitionsGrid.setItems(timeEntryService.findTransitions(entryId, currentUserId));
            refreshNotes();
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
        form.add(workDate, createdAt, hours, description);
        form.setColspan(description, 2);
        form.add(new HorizontalLayout(save, cancel, delete));
        form.setMaxWidth("600px");
        return form;
    }

    private VerticalLayout buildTransitionsSection() {
        transitionsGrid.addColumn(TimeEntryTransitionDto::oldStatusName).setHeader("Old status");
        transitionsGrid.addColumn(TimeEntryTransitionDto::newStatusName).setHeader("New status");
        transitionsGrid.addColumn(TimeEntryTransitionDto::changedByFullName).setHeader("Changed by");
        transitionsGrid.addColumn(t -> Formatters.timestamp(t.createdAt())).setHeader("Changed at")
                .setAutoWidth(true).setFlexGrow(0);
        transitionsGrid.setAllRowsVisible(true);
        transitionsGrid.setWidth("600px");

        VerticalLayout section = new VerticalLayout(new H3("Status history"), transitionsGrid);
        section.setPadding(false);
        return section;
    }

    private VerticalLayout buildNotesSection() {
        notesContainer.setPadding(false);
        notesContainer.setSpacing(false);
        notesContainer.setWidth("600px");

        Button addNote = new Button("Add note", e -> addNote());
        addNote.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout section = new VerticalLayout(new H3("Notes"), notesContainer, newNoteText, addNote);
        section.setPadding(false);
        return section;
    }

    private void addNote() {
        if (newNoteText.getValue() == null || newNoteText.getValue().isBlank()) {
            Notifications.showError("Note text is required");
            return;
        }
        try {
            timeEntryService.addNote(entryId, currentUserId, new CreateTimeEntryNoteRequest(newNoteText.getValue()));
            newNoteText.clear();
            refreshNotes();
            Notification.show("Note added").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (IllegalArgumentException | AccessDeniedException ex) {
            Notifications.showError(ex.getMessage());
        }
    }

    private void refreshNotes() {
        notesContainer.removeAll();
        List<TimeEntryNoteDto> notes = timeEntryService.findNotes(entryId, currentUserId);
        if (notes.isEmpty()) {
            Span empty = new Span("No notes yet.");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
            notesContainer.add(empty);
        }
        notes.forEach(note -> notesContainer.add(buildNoteCard(note)));
    }

    private Div buildNoteCard(TimeEntryNoteDto note) {
        Span author = new Span(note.authorFullName());
        author.getStyle().set("font-weight", "600");

        Span timestamp = new Span(Formatters.timestamp(note.createdAt()));
        timestamp.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "0.875rem");

        HorizontalLayout header = new HorizontalLayout(author, timestamp);
        header.setSpacing(true);

        Div text = new Div();
        text.setText(note.text());
        text.getStyle().set("white-space", "pre-wrap").set("margin-top", "0.25rem");

        Div card = new Div(header, text);
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "0.75rem")
                .set("margin-bottom", "0.5rem");
        return card;
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
