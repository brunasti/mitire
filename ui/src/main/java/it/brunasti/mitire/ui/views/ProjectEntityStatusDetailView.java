package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.service.ProjectEntityStatusService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.ProjectEntityStatusDto;
import it.brunasti.mitire.backend.web.dto.UpdateProjectEntityStatusRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import it.brunasti.mitire.ui.util.Notifications;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.NoSuchElementException;

@Route(value = "statuses", layout = MainLayout.class)
@PageTitle("Status details | MiTiRe")
@RolesAllowed("ADMIN")
public class ProjectEntityStatusDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ProjectEntityStatusService projectEntityStatusService;

    private final Span statusNameLabel = new Span();
    private final RouterLink backLink = new RouterLink();
    private final TextField sequence = new TextField("Order");
    private final TextField name = new TextField("Name");
    private final TextField description = new TextField("Description");
    private final Checkbox active = new Checkbox("Active");
    private final HorizontalLayout startingRow = new HorizontalLayout();
    private final Div parentsField = new Div();
    private final ComboBox<ProjectEntityStatusDto> addChild = new ComboBox<>("Add depending status");

    private final Grid<ProjectEntityStatusDto> childrenGrid = new Grid<>(ProjectEntityStatusDto.class, false);

    private Grid.Column<ProjectEntityStatusDto> childActionsColumn;

    private final Long currentUserId;

    private Long statusId;
    private Long projectId;
    private boolean startingStatus;

    public ProjectEntityStatusDetailView(ProjectEntityStatusService projectEntityStatusService, UserService userService,
                                          AuthenticationContext authenticationContext) {
        this.projectEntityStatusService = projectEntityStatusService;
        this.currentUserId = authenticationContext.getPrincipalName()
                .map(userService::getByUsername)
                .map(UserDto::id)
                .orElseThrow();

        setSizeFull();

        sequence.setReadOnly(true);
        addChild.setItemLabelGenerator(ProjectEntityStatusDto::name);

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Status details", buildDetailsTab());
        tabSheet.add("Depending statuses", buildChildrenTab());
        tabSheet.setSizeFull();

        backLink.setText("← Back to project");
        statusNameLabel.getStyle().set("font-weight", "bold").set("margin-left", "1rem");
        HorizontalLayout header = new HorizontalLayout(backLink, statusNameLabel);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        add(header, tabSheet);
        setFlexGrow(1, tabSheet);
    }

    @Override
    public void setParameter(BeforeEvent event, Long statusId) {
        this.statusId = statusId;
        ProjectEntityStatusDto status;
        try {
            status = projectEntityStatusService.findById(statusId);
        } catch (NoSuchElementException ex) {
            event.rerouteToError(NotFoundException.class, "Status not found");
            return;
        }
        this.projectId = status.projectId();
        this.startingStatus = status.startingStatus();
        backLink.setRoute(ProjectDetailView.class, projectId);
        sequence.setValue(String.valueOf(status.sequence()));
        name.setValue(status.name());
        description.setValue(status.description() != null ? status.description() : "");
        active.setValue(status.active());
        statusNameLabel.setText(status.name());
        refreshStartingRow();
        refreshParents();

        refreshChildren();
    }

    private FormLayout buildDetailsTab() {
        Button save = new Button("Save", e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button delete = new Button("Delete", e -> confirmDelete());
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        FormLayout form = new FormLayout();
        form.add(sequence, name, description, active, startingRow);
        form.addFormItem(parentsField, "Parent status(es)");
        form.add(new HorizontalLayout(save, delete));
        form.setMaxWidth("600px");
        return form;
    }

    private void refreshParents() {
        parentsField.removeAll();
        List<ProjectEntityStatusDto> parents = projectEntityStatusService.findParents(projectId, statusId);
        if (parents.isEmpty()) {
            parentsField.add(new Span("None"));
            return;
        }
        for (ProjectEntityStatusDto parent : parents) {
            RouterLink link = new RouterLink(parent.name(), ProjectEntityStatusDetailView.class, parent.id());
            Div line = new Div(link);
            parentsField.add(line);
        }
    }

    private void refreshStartingRow() {
        startingRow.removeAll();
        if (startingStatus) {
            Icon star = VaadinIcon.STAR.create();
            star.setColor("var(--lumo-primary-color)");
            startingRow.add(star, new Span("This is the starting status"));
        } else {
            Button setStarting = new Button("Set as starting status", VaadinIcon.STAR_O.create());
            setStarting.addClickListener(e -> {
                projectEntityStatusService.setStarting(projectId, statusId, currentUserId);
                startingStatus = true;
                refreshStartingRow();
                Notification.show("Starting status updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            });
            startingRow.add(setStarting);
        }
        startingRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
    }

    private void save() {
        if (name.getValue().isBlank()) {
            Notifications.showError("Name is required");
            return;
        }
        try {
            ProjectEntityStatusDto updated = projectEntityStatusService.update(projectId, statusId,
                    new UpdateProjectEntityStatusRequest(name.getValue(), description.getValue(), active.getValue()),
                    currentUserId);
            statusNameLabel.setText(updated.name());
            Notification.show("Status updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (IllegalArgumentException ex) {
            Notifications.showError(ex.getMessage());
        }
    }

    private void confirmDelete() {
        ConfirmDialog dialog = new ConfirmDialog(
                "Delete status",
                "Delete status '" + name.getValue() + "'? This can't be undone.",
                "Delete", e -> delete(),
                "Cancel", e -> { }
        );
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    private void delete() {
        try {
            projectEntityStatusService.delete(projectId, statusId, currentUserId);
            Notification.show("Status deleted").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().navigate(ProjectDetailView.class, projectId);
        } catch (IllegalArgumentException ex) {
            Notifications.showError(ex.getMessage());
        }
    }

    private VerticalLayout buildChildrenTab() {
        Button add = new Button("Add", e -> addChildLink());
        HorizontalLayout addForm = new HorizontalLayout(addChild, add);
        addForm.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        childrenGrid.addColumn(ProjectEntityStatusDto::sequence).setHeader("Order");
        childrenGrid.addColumn(ProjectEntityStatusDto::name).setHeader("Name");
        childrenGrid.addColumn(ProjectEntityStatusDto::description).setHeader("Description");
        childActionsColumn = childrenGrid.addComponentColumn(this::buildRemoveChildButton).setHeader("").setFlexGrow(0);
        childrenGrid.setSizeFull();
        childrenGrid.getStyle().set("cursor", "pointer");
        childrenGrid.addItemClickListener(e -> {
            if (e.getColumn() != childActionsColumn) {
                UI.getCurrent().navigate(ProjectEntityStatusDetailView.class, e.getItem().id());
            }
        });

        VerticalLayout layout = new VerticalLayout(addForm, childrenGrid);
        layout.setSizeFull();
        return layout;
    }

    private void addChildLink() {
        ProjectEntityStatusDto selected = addChild.getValue();
        if (selected == null) {
            Notifications.showError("Select a status to add");
            return;
        }
        projectEntityStatusService.addChild(projectId, statusId, selected.id(), currentUserId);
        refreshChildren();
        Notification.show("Depending status added").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private Button buildRemoveChildButton(ProjectEntityStatusDto child) {
        Button button = new Button(VaadinIcon.TRASH.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        button.setTooltipText("Remove dependency");
        button.addClickListener(e -> confirmRemoveChild(child));
        return button;
    }

    private void confirmRemoveChild(ProjectEntityStatusDto child) {
        ConfirmDialog dialog = new ConfirmDialog(
                "Remove dependency",
                "'" + child.name() + "' will no longer be reachable from this status. Continue?",
                "Remove", e -> removeChild(child),
                "Cancel", e -> { }
        );
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    private void removeChild(ProjectEntityStatusDto child) {
        projectEntityStatusService.removeChild(projectId, statusId, child.id(), currentUserId);
        refreshChildren();
        Notification.show("Dependency removed").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void refreshChildren() {
        List<ProjectEntityStatusDto> children = projectEntityStatusService.findChildren(projectId, statusId);
        childrenGrid.setItems(children);
        List<ProjectEntityStatusDto> allStatuses = projectEntityStatusService.findByProject(projectId);
        addChild.clear();
        addChild.setItems(allStatuses.stream()
                .filter(s -> !s.id().equals(statusId))
                .filter(s -> children.stream().noneMatch(c -> c.id().equals(s.id())))
                .toList());
    }
}
