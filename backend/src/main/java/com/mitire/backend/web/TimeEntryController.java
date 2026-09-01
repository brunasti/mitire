package com.mitire.backend.web;

import com.mitire.backend.service.TimeEntryService;
import com.mitire.backend.web.dto.CreateTimeEntryRequest;
import com.mitire.backend.web.dto.TimeEntryDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/time-entries")
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    public TimeEntryController(TimeEntryService timeEntryService) {
        this.timeEntryService = timeEntryService;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimeEntryDto create(@Valid @RequestBody CreateTimeEntryRequest request) {
        return timeEntryService.create(request);
    }
}
