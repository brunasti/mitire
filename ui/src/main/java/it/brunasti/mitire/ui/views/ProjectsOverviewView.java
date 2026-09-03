package it.brunasti.mitire.ui.views;

import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.UserDto;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "projects-overview", layout = MainLayout.class)
@PageTitle("Projects | MiTiRe")
@RolesAllowed("VIEWER")
public class ProjectsOverviewView extends VerticalLayout {

    public ProjectsOverviewView(ProjectService projectService, UserService userService,
                                 AuthenticationContext authenticationContext) {
        Long currentUserId = authenticationContext.getPrincipalName()
                .map(userService::getByUsername)
                .map(UserDto::id)
                .orElseThrow();

        setSizeFull();

        H2 title = new H2("Projects");

        Grid<ProjectDto> grid = new Grid<>(ProjectDto.class, false);
        grid.addColumn(ProjectDto::code).setHeader("Code").setSortable(true);
        grid.addColumn(ProjectDto::name).setHeader("Name").setSortable(true);
        grid.addColumn(ProjectDto::active).setHeader("Active").setSortable(true);
        grid.addColumn(ProjectDto::startDate).setHeader("Start date").setSortable(true);
        grid.addColumn(ProjectDto::endDate).setHeader("End date").setSortable(true);
        grid.setItems(userService.findAccessibleProjects(currentUserId));
        grid.setSizeFull();

        add(title, grid);
        setFlexGrow(1, grid);
    }
}
