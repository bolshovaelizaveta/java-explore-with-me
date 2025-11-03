package ru.practicum.main.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.main.dto.*;
import ru.practicum.main.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable,
                                        String sort, int from, int size, HttpServletRequest request);

    EventFullDto getPublicEventById(Long eventId, HttpServletRequest request);

    List<EventShortDto> getPrivateEventsByUser(Long userId, int from, int size);

    EventFullDto createPrivateEvent(Long userId, NewEventDto newEventDto);

    EventFullDto getPrivateEventById(Long userId, Long eventId);

    EventFullDto updatePrivateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);

    List<EventFullDto> getAdminEvents(List<Long> users, List<EventState> states, List<Long> categories,
                                      LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);

    EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest updateEventAdminRequest);
}