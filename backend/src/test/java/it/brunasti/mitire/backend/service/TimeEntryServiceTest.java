package it.brunasti.mitire.backend.service;

import it.brunasti.mitire.backend.domain.Project;
import it.brunasti.mitire.backend.domain.TimeEntry;
import it.brunasti.mitire.backend.domain.User;
import it.brunasti.mitire.backend.repository.TimeEntryRepository;
import it.brunasti.mitire.backend.web.dto.CreateTimeEntryRequest;
import it.brunasti.mitire.backend.web.dto.TimeEntryDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    void createPersistsAndReturnsDto() {
        TimeEntryService service = new TimeEntryService(timeEntryRepository, userService, projectService);

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        Project project = new Project();
        project.setId(2L);
        project.setCode("ACME");

        when(userService.getReference(1L)).thenReturn(user);
        when(projectService.getReference(2L)).thenReturn(project);
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(invocation -> {
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
}
