package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.service.ProjectEntityStatusService;
import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.CreateProjectEntityStatusRequest;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.ProjectEntityStatusDto;
import it.brunasti.mitire.backend.web.dto.UpdateProjectEntityStatusRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Minimal workflow-editing page for a project's Owner (or ADMIN), separate from the
 * ADMIN-only /projects/{id} page — a project Owner who isn't an ADMIN has no access to
 * that page, so this gives them a self-contained place to manage their project's
 * statuses and the dependencies between them.
 */
@Route(value = "project-workflow", layout = MainLayout.class)
@PageTitle("Project workflow | MiTiRe")
@PermitAll
public class ProjectWorkflowView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ProjectService projectService;
    private final ProjectEntityStatusService projectEntityStatusService;
    private final Long currentUserId;
    private final Role currentUserRole;

    private final Span projectNameLabel = new Span();
    private final Grid<ProjectEntityStatusDto> statusesGrid = new Grid<>(ProjectEntityStatusDto.class, false);

    private Long projectId;

    public ProjectWorkflowView(ProjectService projectService, ProjectEntityStatusService projectEntityStatusService,
                                UserService userService, AuthenticationContext authenticationContext) {
        this.projectService = projectService;
        this.projectEntityStatusService = projectEntityStatusService;
        UserDto currentUser = authenticationContext.getPrincipalName()
                .map(userService::getByUsername)
                .orElseThrow();
        this.currentUserId = currentUser.id();
        this.currentUserRole = currentUser.role();

        setSizeFull();

        projectNameLabel.getStyle().set("font-weight", "bold").set("margin-left", "1rem");
        HorizontalLayout header = new HorizontalLayout(
                new RouterLink("← Back to my projects", MyProjectsView.class), projectNameLabel);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        VerticalLayout content = buildStatusesSection();

        add(header, content);
        setFlexGrow(1, content);
    }

    @Override
    public void setParameter(BeforeEvent event, Long projectId) {
        this.projectId = projectId;
        ProjectDto project;
        try {
            project = projectService.findById(projectId);
        } catch (NoSuchElementException ex) {
            event.rerouteToError(NotFoundException.class, "Project not found");
            return;
        }
        boolean isOwner = project.ownerId() != null && project.ownerId().equals(currentUserId);
        if (currentUserRole != Role.ADMIN && !isOwner) {
            event.rerouteToError(AccessDeniedException.class);
            return;
        }
        projectNameLabel.setText(project.name());
        refreshStatuses();
    }

    private VerticalLayout buildStatusesSection() {
        TextField newStatusName = new TextField("Name");
        TextField newStatusDescription = new TextField("Description");
        Button add = new Button("Add", e -> addStatus(newStatusName, newStatusDescription));
        HorizontalLayout addForm = new HorizontalLayout(newStatusName, newStatusDescription, add);
        addForm.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        statusesGrid.addColumn(ProjectEntityStatusDto::sequence).setHeader("Order");
        statusesGrid.addColumn(ProjectEntityStatusDto::name).setHeader("Name");
        statusesGrid.addColumn(ProjectEntityStatusDto::description).setHeader("Description");
        statusesGrid.addColumn(ProjectEntityStatusDto::active).setHeader("Active");
        statusesGrid.addColumn(ProjectEntityStatusDto::startingStatus).setHeader("Starting");
        statusesGrid.addComponentColumn(this::buildStatusActions).setHeader("").setFlexGrow(0);
        statusesGrid.setSizeFull();

        VerticalLayout layout = new VerticalLayout(addForm, statusesGrid);
        layout.setSizeFull();
        return layout;
    }

    private void addStatus(TextField newStatusName, TextField newStatusDescription) {
        if (newStatusName.getValue().isBlank()) {
            Notifications.showError("Name is required");
            return;
        }
        try {
            projectEntityStatusService.create(projectId,
                    new CreateProjectEntityStatusRequest(newStatusName.getValue(), newStatusDescription.getValue()),
                    currentUserId);
            newStatusName.clear();
            newStatusDescription.clear();
            refreshStatuses();
            Notification.show("Status added").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (IllegalArgumentException ex) {
            Notifications.showError(ex.getMessage());
        }
    }

    private HorizontalLayout buildStatusActions(ProjectEntityStatusDto status) {
        Button up = new Button(VaadinIcon.ARROW_UP.create());
        up.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        up.setTooltipText("Move up");
        up.addClickListener(e -> {
            projectEntityStatusService.moveUp(projectId, status.id(), currentUserId);
            refreshStatuses();
        });

        Button down = new Button(VaadinIcon.ARROW_DOWN.create());
        down.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        down.setTooltipText("Move down");
        down.addClickListener(e -> {
            projectEntityStatusService.moveDown(projectId, status.id(), currentUserId);
            refreshStatuses();
        });

        Button edit = new Button(VaadinIcon.EDIT.create());
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        edit.setTooltipText("Edit status");
        edit.addClickListener(e -> openEditStatusDialog(status));

        Button dependencies = new Button(VaadinIcon.CONNECT.create());
        dependencies.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        dependencies.setTooltipText("Manage depending statuses");
        dependencies.addClickListener(e -> openDependenciesDialog(status));

        Button delete = new Button(VaadinIcon.TRASH.create());
        delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        delete.setTooltipText("Delete status");
        delete.addClickListener(e -> confirmDeleteStatus(status));

        HorizontalLayout actions = new HorizontalLayout(up, down, edit, dependencies, delete);

        if (status.startingStatus()) {
            Icon starIcon = VaadinIcon.STAR.create();
            starIcon.setColor("var(--lumo-primary-color)");
            starIcon.setTooltipText("This is the starting status");
            actions.addComponentAsFirst(starIcon);
        } else {
            Button setStarting = new Button(VaadinIcon.STAR_O.create());
            setStarting.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
            setStarting.setTooltipText("Set as starting status");
            setStarting.addClickListener(e -> {
                projectEntityStatusService.setStarting(projectId, status.id(), currentUserId);
                refreshStatuses();
            });
            actions.addComponentAsFirst(setStarting);
        }

        return actions;
    }

    private void openEditStatusDialog(ProjectEntityStatusDto status) {
        TextField editName = new TextField("Name");
        editName.setValue(status.name());
        TextField editDescription = new TextField("Description");
        editDescription.setValue(status.description() != null ? status.description() : "");
        Checkbox editActive = new Checkbox("Active");
        editActive.setValue(status.active());

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit status");

        Button save = new Button("Save", e -> {
            try {
                projectEntityStatusService.update(projectId, status.id(), new UpdateProjectEntityStatusRequest(
                        editName.getValue(), editDescription.getValue(), editActive.getValue()), currentUserId);
                refreshStatuses();
                dialog.close();
                Notification.show("Status updated").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                Notifications.showError(ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(new FormLayout(editName, editDescription, editActive));
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void openDependenciesDialog(ProjectEntityStatusDto status) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Depending statuses for '" + status.name() + "'");

        Grid<ProjectEntityStatusDto> childrenGrid = new Grid<>(ProjectEntityStatusDto.class, false);
        ComboBox<ProjectEntityStatusDto> addChild = new ComboBox<>("Add depending status");
        addChild.setItemLabelGenerator(ProjectEntityStatusDto::name);

        childrenGrid.addColumn(ProjectEntityStatusDto::sequence).setHeader("Order");
        childrenGrid.addColumn(ProjectEntityStatusDto::name).setHeader("Name");
        childrenGrid.addComponentColumn(child -> {
            Button remove = new Button(VaadinIcon.TRASH.create());
            remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            remove.setTooltipText("Remove dependency");
            remove.addClickListener(e -> {
                projectEntityStatusService.removeChild(projectId, status.id(), child.id(), currentUserId);
                loadDependencies(status, childrenGrid, addChild);
            });
            return remove;
        }).setHeader("").setFlexGrow(0);
        childrenGrid.setWidthFull();
        childrenGrid.setHeight("250px");

        Button add = new Button("Add", e -> {
            ProjectEntityStatusDto selected = addChild.getValue();
            if (selected == null) {
                Notifications.showError("Select a status to add");
                return;
            }
            projectEntityStatusService.addChild(projectId, status.id(), selected.id(), currentUserId);
            loadDependencies(status, childrenGrid, addChild);
        });
        HorizontalLayout addForm = new HorizontalLayout(addChild, add);
        addForm.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        VerticalLayout content = new VerticalLayout(childrenGrid, addForm);
        content.setPadding(false);
        dialog.add(content);

        Button close = new Button("Close", e -> {
            dialog.close();
            refreshStatuses();
        });
        dialog.getFooter().add(close);

        loadDependencies(status, childrenGrid, addChild);
        dialog.open();
    }

    private void loadDependencies(ProjectEntityStatusDto status, Grid<ProjectEntityStatusDto> childrenGrid,
                                   ComboBox<ProjectEntityStatusDto> addChild) {
        List<ProjectEntityStatusDto> children = projectEntityStatusService.findChildren(projectId, status.id());
        childrenGrid.setItems(children);
        List<ProjectEntityStatusDto> allStatuses = projectEntityStatusService.findByProject(projectId);
        addChild.clear();
        addChild.setItems(allStatuses.stream()
                .filter(s -> !s.id().equals(status.id()))
                .filter(s -> children.stream().noneMatch(c -> c.id().equals(s.id())))
                .toList());
    }

    private void confirmDeleteStatus(ProjectEntityStatusDto status) {
        ConfirmDialog dialog = new ConfirmDialog(
                "Delete status",
                "Delete status '" + status.name() + "'? This can't be undone.",
                "Delete", e -> deleteStatus(status),
                "Cancel", e -> { }
        );
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    private void deleteStatus(ProjectEntityStatusDto status) {
        try {
            projectEntityStatusService.delete(projectId, status.id(), currentUserId);
            refreshStatuses();
            Notification.show("Status deleted").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (IllegalArgumentException ex) {
            Notifications.showError(ex.getMessage());
        }
    }

    private void refreshStatuses() {
        statusesGrid.setItems(projectEntityStatusService.findByProject(projectId));
    }
}
