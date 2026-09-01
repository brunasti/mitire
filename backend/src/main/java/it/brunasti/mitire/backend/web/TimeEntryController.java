package it.brunasti.mitire.backend.web;

import it.brunasti.mitire.backend.service.TimeEntryService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.CreateTimeEntryRequest;
import it.brunasti.mitire.backend.web.dto.TimeEntryDto;
import it.brunasti.mitire.backend.web.dto.UpdateTimeEntryRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/time-entries")
public class TimeEntryController {

    private final TimeEntryService timeEntryService;
    private final UserService userService;

    public TimeEntryController(TimeEntryService timeEntryService, UserService userService) {
        this.timeEntryService = timeEntryService;
        this.userService = userService;
    }

    @GetMapping
    public List<TimeEntryDto> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return timeEntryService.search(userId, projectId, from, to);
    }

    @GetMapping("/{id}")
    public TimeEntryDto findById(@PathVariable Long id, Authentication authentication) {
        return timeEntryService.findByIdForUser(id, resolveUserId(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimeEntryDto create(@Valid @RequestBody CreateTimeEntryRequest request) {
        return timeEntryService.create(request);
    }

    @PutMapping("/{id}")
    public TimeEntryDto update(@PathVariable Long id, Authentication authentication,
                                @Valid @RequestBody UpdateTimeEntryRequest request) {
        return timeEntryService.update(id, resolveUserId(authentication), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        timeEntryService.delete(id, resolveUserId(authentication));
    }

    private Long resolveUserId(Authentication authentication) {
        return userService.getByUsername(authentication.getName()).id();
    }
}
