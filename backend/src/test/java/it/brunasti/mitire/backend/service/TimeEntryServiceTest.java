package it.brunasti.mitire.backend.service;

import it.brunasti.mitire.backend.domain.Group;
import it.brunasti.mitire.backend.domain.Project;
import it.brunasti.mitire.backend.domain.ProjectEntryStatus;
import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.domain.TimeEntry;
import it.brunasti.mitire.backend.domain.User;
import it.brunasti.mitire.backend.repository.TimeEntryNoteRepository;
import it.brunasti.mitire.backend.repository.TimeEntryRepository;
import it.brunasti.mitire.backend.repository.TimeEntryTransitionRepository;
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
    private TimeEntryTransitionRepository timeEntryTransitionRepository;
    @Mock
    private TimeEntryNoteRepository timeEntryNoteRepository;
    @Mock
    private UserService userService;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectEntryStatusService projectEntryStatusService;

    private static ProjectEntryStatus defaultStatus(Project project) {
        ProjectEntryStatus status = new ProjectEntryStatus();
        status.setId(500L);
        status.setProject(project);
        status.setName("SUBMITTED");
        status.setSequence(1);
        return status;
    }

    @Test
    void createPersistsAndReturnsDtoWhenUserHasProjectAccessThroughGroup() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, timeEntryTransitionRepository, timeEntryNoteRepository, userService, projectService, projectEntryStatusService);

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
        when(projectEntryStatusService.getDefaultForProject(2L)).thenReturn(defaultStatus(project));
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
        TimeEntryService service = new TimeEntryService(timeEntryRepository, timeEntryTransitionRepository, timeEntryNoteRepository, userService, projectService, projectEntryStatusService);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);

        when(userService.getReference(1L)).thenReturn(admin);
        when(projectService.getReference(2L)).thenReturn(project);
        when(projectEntryStatusService.getDefaultForProject(2L)).thenReturn(defaultStatus(project));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateTimeEntryRequest request = new CreateTimeEntryRequest(1L, 2L, LocalDate.of(2026, 8, 20), new BigDecimal("2"), null);

        assertThat(service.create(request)).isNotNull();
    }

    @Test
    void createRejectsForViewerInAViewerRoleGroup() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, timeEntryTransitionRepository, timeEntryNoteRepository, userService, projectService, projectEntryStatusService);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        Group group = new Group();
        group.setId(10L);
        group.setName("Team A");
        group.setRole(Role.VIEWER);
        group.setProjects(Set.of(project));

        User viewer = new User();
        viewer.setId(1L);
        viewer.setUsername("vic");
        viewer.setRole(Role.VIEWER);
        viewer.setGroups(Set.of(group));

        when(userService.getReference(1L)).thenReturn(viewer);
        when(projectService.getReference(2L)).thenReturn(project);

        CreateTimeEntryRequest request = new CreateTimeEntryRequest(1L, 2L, LocalDate.of(2026, 8, 20), new BigDecimal("2"), null);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createSucceedsForViewerElevatedByMemberRoleGroup() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, timeEntryTransitionRepository, timeEntryNoteRepository, userService, projectService, projectEntryStatusService);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        Group group = new Group();
        group.setId(10L);
        group.setName("Team A");
        group.setRole(Role.MEMBER);
        group.setProjects(Set.of(project));

        User viewer = new User();
        viewer.setId(1L);
        viewer.setUsername("vic");
        viewer.setRole(Role.VIEWER);
        viewer.setGroups(Set.of(group));

        when(userService.getReference(1L)).thenReturn(viewer);
        when(projectService.getReference(2L)).thenReturn(project);
        when(projectEntryStatusService.getDefaultForProject(2L)).thenReturn(defaultStatus(project));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateTimeEntryRequest request = new CreateTimeEntryRequest(1L, 2L, LocalDate.of(2026, 8, 20), new BigDecimal("2"), null);

        assertThat(service.create(request)).isNotNull();
    }

    @Test
    void updateStatusSucceedsForNonAdminUserElevatedToAdminByGroup() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, timeEntryTransitionRepository, timeEntryNoteRepository, userService, projectService, projectEntryStatusService);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        Group group = new Group();
        group.setId(10L);
        group.setName("Leads");
        group.setRole(Role.ADMIN);
        group.setProjects(Set.of(project));

        User approver = new User();
        approver.setId(1L);
        approver.setUsername("lead");
        approver.setRole(Role.MEMBER);
        approver.setGroups(Set.of(group));

        User entryOwner = new User();
        entryOwner.setId(2L);
        entryOwner.setUsername("alice");

        ProjectEntryStatus newStatus = new ProjectEntryStatus();
        newStatus.setId(600L);
        newStatus.setProject(project);
        newStatus.setName("APPROVED");

        TimeEntry entry = new TimeEntry();
        entry.setId(99L);
        entry.setUser(entryOwner);
        entry.setProject(project);
        entry.setWorkDate(LocalDate.of(2026, 8, 20));
        entry.setHours(new BigDecimal("5"));
        entry.setStatus(defaultStatus(project));

        when(userService.getReference(1L)).thenReturn(approver);
        when(timeEntryRepository.findById(99L)).thenReturn(java.util.Optional.of(entry));
        when(timeEntryRepository.findByUserIdAndWorkDate(2L, LocalDate.of(2026, 8, 20))).thenReturn(List.of());
        when(projectEntryStatusService.getReference(600L)).thenReturn(newStatus);
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        it.brunasti.mitire.backend.web.dto.UpdateTimeEntryRequest request =
                new it.brunasti.mitire.backend.web.dto.UpdateTimeEntryRequest(new BigDecimal("5"), null, 600L);

        TimeEntryDto dto = service.update(99L, 1L, request);

        assertThat(dto.statusId()).isEqualTo(600L);
    }

    @Test
    void updateStatusRecordsTransitionWithOldStatusNewStatusAndChangedByUser() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, timeEntryTransitionRepository, timeEntryNoteRepository, userService, projectService, projectEntryStatusService);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);

        ProjectEntryStatus oldStatus = defaultStatus(project);
        ProjectEntryStatus newStatus = new ProjectEntryStatus();
        newStatus.setId(600L);
        newStatus.setProject(project);
        newStatus.setName("APPROVED");

        TimeEntry entry = new TimeEntry();
        entry.setId(99L);
        entry.setUser(admin);
        entry.setProject(project);
        entry.setWorkDate(LocalDate.of(2026, 8, 20));
        entry.setHours(new BigDecimal("5"));
        entry.setStatus(oldStatus);

        when(userService.getReference(1L)).thenReturn(admin);
        when(timeEntryRepository.findById(99L)).thenReturn(java.util.Optional.of(entry));
        when(timeEntryRepository.findByUserIdAndWorkDate(1L, LocalDate.of(2026, 8, 20))).thenReturn(List.of());
        when(projectEntryStatusService.getReference(600L)).thenReturn(newStatus);
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        it.brunasti.mitire.backend.web.dto.UpdateTimeEntryRequest request =
                new it.brunasti.mitire.backend.web.dto.UpdateTimeEntryRequest(new BigDecimal("5"), null, 600L);

        service.update(99L, 1L, request);

        org.mockito.ArgumentCaptor<it.brunasti.mitire.backend.domain.TimeEntryTransition> captor =
                org.mockito.ArgumentCaptor.forClass(it.brunasti.mitire.backend.domain.TimeEntryTransition.class);
        org.mockito.Mockito.verify(timeEntryTransitionRepository).save(captor.capture());

        it.brunasti.mitire.backend.domain.TimeEntryTransition transition = captor.getValue();
        assertThat(transition.getTimeEntry()).isSameAs(entry);
        assertThat(transition.getOldStatus()).isSameAs(oldStatus);
        assertThat(transition.getNewStatus()).isSameAs(newStatus);
        assertThat(transition.getChangedBy()).isSameAs(admin);
    }

    @Test
    void updateWithUnchangedStatusDoesNotRecordTransition() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, timeEntryTransitionRepository, timeEntryNoteRepository, userService, projectService, projectEntryStatusService);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);

        ProjectEntryStatus status = defaultStatus(project);

        TimeEntry entry = new TimeEntry();
        entry.setId(99L);
        entry.setUser(admin);
        entry.setProject(project);
        entry.setWorkDate(LocalDate.of(2026, 8, 20));
        entry.setHours(new BigDecimal("5"));
        entry.setStatus(status);

        when(userService.getReference(1L)).thenReturn(admin);
        when(timeEntryRepository.findById(99L)).thenReturn(java.util.Optional.of(entry));
        when(timeEntryRepository.findByUserIdAndWorkDate(1L, LocalDate.of(2026, 8, 20))).thenReturn(List.of());
        when(projectEntryStatusService.getReference(500L)).thenReturn(status);
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        it.brunasti.mitire.backend.web.dto.UpdateTimeEntryRequest request =
                new it.brunasti.mitire.backend.web.dto.UpdateTimeEntryRequest(new BigDecimal("5"), null, 500L);

        service.update(99L, 1L, request);

        org.mockito.Mockito.verifyNoInteractions(timeEntryTransitionRepository);
    }

    @Test
    void createRejectsWhenUserHasNoAccessToProject() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, timeEntryTransitionRepository, timeEntryNoteRepository, userService, projectService, projectEntryStatusService);

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
        TimeEntryService service = new TimeEntryService(timeEntryRepository, timeEntryTransitionRepository, timeEntryNoteRepository, userService, projectService, projectEntryStatusService);

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
        TimeEntryService service = new TimeEntryService(timeEntryRepository, timeEntryTransitionRepository, timeEntryNoteRepository, userService, projectService, projectEntryStatusService);

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
        TimeEntryService service = new TimeEntryService(timeEntryRepository, timeEntryTransitionRepository, timeEntryNoteRepository, userService, projectService, projectEntryStatusService);

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
                new it.brunasti.mitire.backend.web.dto.UpdateTimeEntryRequest(new BigDecimal("5"), null, null);

        assertThatThrownBy(() -> service.update(99L, 1L, request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findTransitionsReturnsHistoryOrderedNewestFirstForTheOwningUser() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, timeEntryTransitionRepository, timeEntryNoteRepository, userService, projectService, projectEntryStatusService);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setFullName("Alice Anderson");
        user.setRole(Role.MEMBER);

        TimeEntry entry = new TimeEntry();
        entry.setId(99L);
        entry.setUser(user);
        entry.setProject(project);

        ProjectEntryStatus submitted = defaultStatus(project);
        ProjectEntryStatus approved = new ProjectEntryStatus();
        approved.setId(600L);
        approved.setProject(project);
        approved.setName("APPROVED");

        it.brunasti.mitire.backend.domain.TimeEntryTransition transition =
                new it.brunasti.mitire.backend.domain.TimeEntryTransition();
        transition.setId(1L);
        transition.setTimeEntry(entry);
        transition.setOldStatus(submitted);
        transition.setNewStatus(approved);
        transition.setChangedBy(user);

        when(userService.getReference(1L)).thenReturn(user);
        when(timeEntryRepository.findById(99L)).thenReturn(java.util.Optional.of(entry));
        when(timeEntryTransitionRepository.findByTimeEntryIdOrderByCreatedAtDesc(99L))
                .thenReturn(List.of(transition));

        List<it.brunasti.mitire.backend.web.dto.TimeEntryTransitionDto> history = service.findTransitions(99L, 1L);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).oldStatusName()).isEqualTo("SUBMITTED");
        assertThat(history.get(0).newStatusName()).isEqualTo("APPROVED");
        assertThat(history.get(0).changedByFullName()).isEqualTo("Alice Anderson");
    }

    @Test
    void findTransitionsRejectsUserWithoutAccessToTheEntry() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, timeEntryTransitionRepository, timeEntryNoteRepository, userService, projectService, projectEntryStatusService);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        User owner = new User();
        owner.setId(1L);
        owner.setUsername("alice");
        owner.setRole(Role.MEMBER);

        User stranger = new User();
        stranger.setId(2L);
        stranger.setUsername("bob");
        stranger.setRole(Role.MEMBER);

        TimeEntry entry = new TimeEntry();
        entry.setId(99L);
        entry.setUser(owner);
        entry.setProject(project);

        when(userService.getReference(2L)).thenReturn(stranger);
        when(timeEntryRepository.findById(99L)).thenReturn(java.util.Optional.of(entry));

        assertThatThrownBy(() -> service.findTransitions(99L, 2L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void addNoteSavesNoteAuthoredByRequestingUser() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, timeEntryTransitionRepository, timeEntryNoteRepository, userService, projectService, projectEntryStatusService);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        User author = new User();
        author.setId(1L);
        author.setUsername("alice");
        author.setFullName("Alice Anderson");
        author.setRole(Role.MEMBER);

        TimeEntry entry = new TimeEntry();
        entry.setId(99L);
        entry.setUser(author);
        entry.setProject(project);

        when(userService.getReference(1L)).thenReturn(author);
        when(timeEntryRepository.findById(99L)).thenReturn(java.util.Optional.of(entry));
        when(timeEntryNoteRepository.save(any(it.brunasti.mitire.backend.domain.TimeEntryNote.class)))
                .thenAnswer(invocation -> {
                    it.brunasti.mitire.backend.domain.TimeEntryNote note = invocation.getArgument(0);
                    note.setId(1L);
                    return note;
                });

        it.brunasti.mitire.backend.web.dto.CreateTimeEntryNoteRequest request =
                new it.brunasti.mitire.backend.web.dto.CreateTimeEntryNoteRequest("please review by Friday");

        it.brunasti.mitire.backend.web.dto.TimeEntryNoteDto dto = service.addNote(99L, 1L, request);

        assertThat(dto.text()).isEqualTo("please review by Friday");
        assertThat(dto.authorId()).isEqualTo(1L);
        assertThat(dto.authorFullName()).isEqualTo("Alice Anderson");
    }

    @Test
    void addNoteRejectsUserWithoutAccessToTheEntry() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, timeEntryTransitionRepository, timeEntryNoteRepository, userService, projectService, projectEntryStatusService);

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        User owner = new User();
        owner.setId(1L);
        owner.setUsername("alice");
        owner.setRole(Role.MEMBER);

        User stranger = new User();
        stranger.setId(2L);
        stranger.setUsername("bob");
        stranger.setRole(Role.MEMBER);

        TimeEntry entry = new TimeEntry();
        entry.setId(99L);
        entry.setUser(owner);
        entry.setProject(project);

        when(userService.getReference(2L)).thenReturn(stranger);
        when(timeEntryRepository.findById(99L)).thenReturn(java.util.Optional.of(entry));

        it.brunasti.mitire.backend.web.dto.CreateTimeEntryNoteRequest request =
                new it.brunasti.mitire.backend.web.dto.CreateTimeEntryNoteRequest("sneaky note");

        assertThatThrownBy(() -> service.addNote(99L, 2L, request)).isInstanceOf(AccessDeniedException.class);
    }
}
