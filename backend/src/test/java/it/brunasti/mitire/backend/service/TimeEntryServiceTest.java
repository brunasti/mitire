package it.brunasti.mitire.backend.service;

import it.brunasti.mitire.backend.domain.Group;
import it.brunasti.mitire.backend.domain.Project;
import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.domain.TimeEntry;
import it.brunasti.mitire.backend.domain.User;
import it.brunasti.mitire.backend.repository.TimeEntryRepository;
import it.brunasti.mitire.backend.web.dto.CreateTimeEntryRequest;
import it.brunasti.mitire.backend.web.dto.TimeEntryDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceTest {

    @Mock
    private TimeEntryRepository timeEntryRepository;
    @Mock
    private UserService userService;
    @Mock
    private ProjectService projectService;

    @Test
    void createPersistsAndReturnsDtoWhenUserHasProjectAccessThroughGroup() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, userService, projectService);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        Group group = new Group();
        group.setId(10L);
        group.setName("Team A");
        group.setProjects(Set.of(project));

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole(Role.MEMBER);
        user.setGroups(Set.of(group));

        when(userService.getReference(1L)).thenReturn(user);
        when(projectService.getReference(2L)).thenReturn(project);
        lenient().when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(invocation -> {
            TimeEntry entry = invocation.getArgument(0);
            entry.setId(99L);
            return entry;
        });

        CreateTimeEntryRequest request = new CreateTimeEntryRequest(1L, 2L, LocalDate.of(2026, 8, 20), new BigDecimal("7.5"), "worked on stuff");

        TimeEntryDto dto = service.create(request);

        assertThat(dto.id()).isEqualTo(99L);
        assertThat(dto.username()).isEqualTo("alice");
        assertThat(dto.projectCode()).isEqualTo("ACME");
        assertThat(dto.hours()).isEqualByComparingTo("7.5");
    }

    @Test
    void createSucceedsForAdminRegardlessOfGroup() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, userService, projectService);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);

        when(userService.getReference(1L)).thenReturn(admin);
        when(projectService.getReference(2L)).thenReturn(project);
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateTimeEntryRequest request = new CreateTimeEntryRequest(1L, 2L, LocalDate.of(2026, 8, 20), new BigDecimal("2"), null);

        assertThat(service.create(request)).isNotNull();
    }

    @Test
    void createRejectsWhenUserHasNoAccessToProject() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, userService, projectService);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        User user = new User();
        user.setId(1L);
        user.setUsername("bob");
        user.setRole(Role.MEMBER);

        when(userService.getReference(1L)).thenReturn(user);
        when(projectService.getReference(2L)).thenReturn(project);

        CreateTimeEntryRequest request = new CreateTimeEntryRequest(1L, 2L, LocalDate.of(2026, 8, 20), new BigDecimal("2"), null);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createRejectsWhenDailyTotalWouldExceed24Hours() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, userService, projectService);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);

        TimeEntry existing = new TimeEntry();
        existing.setId(50L);
        existing.setHours(new BigDecimal("20"));

        when(userService.getReference(1L)).thenReturn(admin);
        when(projectService.getReference(2L)).thenReturn(project);
        when(timeEntryRepository.findByUserIdAndWorkDate(1L, LocalDate.of(2026, 8, 20)))
                .thenReturn(List.of(existing));

        CreateTimeEntryRequest request = new CreateTimeEntryRequest(1L, 2L, LocalDate.of(2026, 8, 20), new BigDecimal("5"), null);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsWorkDateOutsideProjectDateRange() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, userService, projectService);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");
        project.setStartDate(LocalDate.of(2026, 1, 1));
        project.setEndDate(LocalDate.of(2026, 6, 30));

        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);

        when(userService.getReference(1L)).thenReturn(admin);
        when(projectService.getReference(2L)).thenReturn(project);

        CreateTimeEntryRequest request = new CreateTimeEntryRequest(1L, 2L, LocalDate.of(2026, 8, 20), new BigDecimal("2"), null);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateRejectsWhenDailyTotalWouldExceed24HoursExcludingItself() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, userService, projectService);

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole(Role.ADMIN);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        TimeEntry entryBeingEdited = new TimeEntry();
        entryBeingEdited.setId(99L);
        entryBeingEdited.setUser(user);
        entryBeingEdited.setProject(project);
        entryBeingEdited.setWorkDate(LocalDate.of(2026, 8, 20));
        entryBeingEdited.setHours(new BigDecimal("10"));

        TimeEntry otherEntrySameDay = new TimeEntry();
        otherEntrySameDay.setId(50L);
        otherEntrySameDay.setHours(new BigDecimal("20"));

        when(userService.getReference(1L)).thenReturn(user);
        when(timeEntryRepository.findById(99L)).thenReturn(java.util.Optional.of(entryBeingEdited));
        when(timeEntryRepository.findByUserIdAndWorkDate(1L, LocalDate.of(2026, 8, 20)))
                .thenReturn(List.of(entryBeingEdited, otherEntrySameDay));

        it.brunasti.mitire.backend.web.dto.UpdateTimeEntryRequest request =
                new it.brunasti.mitire.backend.web.dto.UpdateTimeEntryRequest(new BigDecimal("5"), null);

        assertThatThrownBy(() -> service.update(99L, 1L, request)).isInstanceOf(IllegalArgumentException.class);
    }
}
