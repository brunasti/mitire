package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

@Route(value = "my-projects", layout = MainLayout.class)
@PageTitle("My Projects | MiTiRe")
@PermitAll
public class MyProjectsView extends VerticalLayout {

    public MyProjectsView(ProjectService projectService, UserService userService,
                           AuthenticationContext authenticationContext) {
        Long currentUserId = authenticationContext.getPrincipalName()
                .map(userService::getByUsername)
                .map(UserDto::id)
                .orElseThrow();

        setSizeFull();

        H2 title = new H2("My Projects");

        Grid<ProjectDto> grid = new Grid<>(ProjectDto.class, false);
        grid.addColumn(ProjectDto::code).setHeader("Code").setSortable(true);
        grid.addColumn(ProjectDto::name).setHeader("Name").setSortable(true);
        grid.addColumn(ProjectDto::active).setHeader("Active");
        grid.setItems(projectService.findByOwner(currentUserId));
        grid.setSizeFull();
        grid.getStyle().set("cursor", "pointer");
        grid.addItemClickListener(e -> UI.getCurrent().navigate(ProjectWorkflowView.class, e.getItem().id()));

        add(title, grid);
        setFlexGrow(1, grid);
    }
}
