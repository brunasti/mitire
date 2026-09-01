package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.service.TimeEntryService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.CreateTimeEntryRequest;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.TimeEntryDto;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Time entries | Mitire")
@PermitAll
public class TimeEntriesView extends VerticalLayout {

    private final TimeEntryService timeEntryService;
    private final Long currentUserId;

    private final Grid<TimeEntryDto> grid = new Grid<>(TimeEntryDto.class, false);

    public TimeEntriesView(TimeEntryService timeEntryService, UserService userService,
                            AuthenticationContext authenticationContext) {
        this.timeEntryService = timeEntryService;
        UserDto currentUser = authenticationContext.getPrincipalName()
                .map(userService::getByUsername)
                .orElseThrow();
        this.currentUserId = currentUser.id();

        List<ProjectDto> accessibleProjects = userService.findAccessibleProjects(currentUserId);

        add(buildForm(accessibleProjects));
        add(buildGrid());

        refreshGrid();
    }

    private FormLayout buildForm(List<ProjectDto> accessibleProjects) {
        ComboBox<ProjectDto> project = new ComboBox<>("Project");
        project.setItems(accessibleProjects);
        project.setItemLabelGenerator(p -> p.code() + " - " + p.name());

        DatePicker workDate = new DatePicker("Date", LocalDate.now());
        NumberField hours = new NumberField("Hours");
        hours.setStep(0.25);
        hours.setMin(0.25);
        hours.setMax(24);
        hours.setValue(8.0);

        TextField description = new TextField("Description");

        Button submit = new Button("Submit", e -> {
            if (project.getValue() == null || workDate.getValue() == null || hours.getValue() == null) {
                Notification.show("Please fill in project, date and hours").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                timeEntryService.create(new CreateTimeEntryRequest(
                        currentUserId,
                        project.getValue().id(),
                        workDate.getValue(),
                        BigDecimal.valueOf(hours.getValue()),
                        description.getValue()
                ));
                Notification.show("Time entry saved").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                description.clear();
                refreshGrid();
            } catch (AccessDeniedException ex) {
                Notification.show("You don't have access to that project").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        FormLayout form = new FormLayout(project, workDate, hours, description, submit);
        form.setColspan(description, 2);
        return form;
    }

    private Grid<TimeEntryDto> buildGrid() {
        grid.addColumn(TimeEntryDto::workDate).setHeader("Date").setSortable(true);
        grid.addColumn(TimeEntryDto::projectCode).setHeader("Project").setSortable(true);
        grid.addColumn(TimeEntryDto::hours).setHeader("Hours");
        grid.addColumn(TimeEntryDto::description).setHeader("Description");
        grid.addColumn(TimeEntryDto::status).setHeader("Status");
        grid.setWidthFull();
        grid.setHeight("400px");
        grid.getStyle().set("cursor", "pointer");
        grid.addItemClickListener(e -> UI.getCurrent().navigate(TimeEntryDetailView.class, e.getItem().id()));
        return grid;
    }

    private void refreshGrid() {
        grid.setItems(timeEntryService.search(currentUserId, null, null, null));
    }
}
