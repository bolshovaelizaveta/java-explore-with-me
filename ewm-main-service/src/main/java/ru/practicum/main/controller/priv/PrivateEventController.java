package ru.practicum.main.controller.priv;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.EventFullDto;
import ru.practicum.main.dto.EventShortDto;
import ru.practicum.main.dto.NewEventDto;
import ru.practicum.main.dto.UpdateEventUserRequest;
import ru.practicum.main.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Slf4j
public class PrivateEventController {

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getPrivateEventsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        log.info("PRIVATE-API: Запрос от пользователя id={} на получение своих событий", userId);
        return eventService.getPrivateEventsByUser(userId, from, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createPrivateEvent(
            @PathVariable Long userId,
            @Valid @RequestBody NewEventDto newEventDto) {
        log.info("PRIVATE-API: Запрос от пользователя id={} на создание события: {}", userId, newEventDto);
        return eventService.createPrivateEvent(userId, newEventDto);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getPrivateEventById(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        log.info("PRIVATE-API: Запрос от пользователя id={} на получение своего события id={}", userId, eventId);
        return eventService.getPrivateEventById(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updatePrivateEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest) {
        log.info("PRIVATE-API: Запрос от пользователя id={} на обновление своего события id={}", userId, eventId);
        return eventService.updatePrivateEvent(userId, eventId, updateEventUserRequest);
    }
}