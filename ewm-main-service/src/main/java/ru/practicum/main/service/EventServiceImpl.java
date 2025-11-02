package ru.practicum.main.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.main.dto.*;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.EventMapper;
import ru.practicum.main.model.*;
import ru.practicum.main.model.enums.EventState;
import ru.practicum.main.model.enums.StateActionAdmin;
import ru.practicum.main.model.enums.StateActionUser;
import ru.practicum.main.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable,
                                               String sort, int from, int size, HttpServletRequest request) {

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Дата начала не может быть после даты окончания.");
        }

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(100);
        }

        String searchText = null;
        if (text != null && !text.isBlank()) {
            searchText = "%" + text.toLowerCase() + "%";
        }

        Pageable pageable = PageRequest.of(from / size, size);

        List<Event> events;
        if (onlyAvailable) {
            events = eventRepository.findAvailablePublishedEvents(searchText, categories, paid, rangeStart, rangeEnd, pageable);
        } else {
            events = eventRepository.findPublishedEvents(searchText, categories, paid, rangeStart, rangeEnd, pageable);
        }

        sendHit(request);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> views = getViewsForEvents(events);
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(events);

        List<EventShortDto> eventShortDtos = events.stream()
                .map(event -> eventMapper.toEventShortDto(
                        event,
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        views.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());

        if ("VIEWS".equalsIgnoreCase(sort)) {
            eventShortDtos.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        }

        return eventShortDtos;
    }

    @Override
    public EventFullDto getPublicEventById(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Событие с id=" + eventId + " не опубликовано.");
        }

        sendHit(request);

        Map<Long, Long> views = getViewsForEvents(List.of(event));
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(List.of(event));

        return eventMapper.toEventFullDto(
                event,
                confirmedRequests.getOrDefault(event.getId(), 0L),
                views.getOrDefault(event.getId(), 0L) + 1
        );
    }

    @Override
    public List<EventShortDto> getPrivateEventsByUser(Long userId, int from, int size) {
        checkUserExists(userId);
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        Map<Long, Long> views = getViewsForEvents(events);
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(events);

        return events.stream()
                .map(event -> eventMapper.toEventShortDto(
                        event,
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        views.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto createPrivateEvent(Long userId, NewEventDto newEventDto) {
        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть не ранее, чем через 2 часа от текущего момента.");
        }

        User initiator = findUserById(userId);
        Category category = findCategoryById(newEventDto.getCategory());

        Event event = eventMapper.toEvent(newEventDto, category, initiator);
        Event savedEvent = eventRepository.save(event);

        return eventMapper.toEventFullDto(savedEvent, 0L, 0L);
    }

    @Override
    public EventFullDto getPrivateEventById(Long userId, Long eventId) {
        checkUserExists(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " для пользователя " + userId + " не найдено."));

        Map<Long, Long> views = getViewsForEvents(List.of(event));
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(List.of(event));

        return eventMapper.toEventFullDto(
                event,
                confirmedRequests.getOrDefault(event.getId(), 0L),
                views.getOrDefault(event.getId(), 0L)
        );
    }

    @Override
    @Transactional
    public EventFullDto updatePrivateEvent(Long userId, Long eventId, UpdateEventUserRequest updateDto) {
        checkUserExists(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " для пользователя " + userId + " не найдено."));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменять уже опубликованное событие.");
        }

        updateEventFields(event, updateDto);

        if (updateDto.getStateAction() != null) {
            if (updateDto.getStateAction() == StateActionUser.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            } else if (updateDto.getStateAction() == StateActionUser.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            }
        }

        Event updatedEvent = eventRepository.save(event);
        Map<Long, Long> views = getViewsForEvents(List.of(updatedEvent));
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(List.of(updatedEvent));

        return eventMapper.toEventFullDto(
                updatedEvent,
                confirmedRequests.getOrDefault(updatedEvent.getId(), 0L),
                views.getOrDefault(updatedEvent.getId(), 0L)
        );
    }

    @Override
    public List<EventFullDto> getAdminEvents(List<Long> users, List<EventState> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);

        if (rangeStart == null) rangeStart = LocalDateTime.now().minusYears(100);
        if (rangeEnd == null) rangeEnd = LocalDateTime.now().plusYears(100);

        List<Event> events = eventRepository.findEventsByAdminParams(users, states, categories, rangeStart, rangeEnd, pageable);
        Map<Long, Long> views = getViewsForEvents(events);
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(events);

        return events.stream()
                .map(event -> eventMapper.toEventFullDto(
                        event,
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        views.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest updateDto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));

        updateEventFields(event, updateDto);

        if (updateDto.getStateAction() != null) {
            if (updateDto.getStateAction() == StateActionAdmin.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие можно опубликовать, только если оно в состоянии ожидания публикации.");
                }
                if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new ConflictException("Дата начала события должна быть не ранее чем за час до публикации.");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (updateDto.getStateAction() == StateActionAdmin.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Нельзя отклонить уже опубликованное событие.");
                }
                event.setState(EventState.CANCELED);
            }
        }

        Event updatedEvent = eventRepository.save(event);
        Map<Long, Long> views = getViewsForEvents(List.of(updatedEvent));
        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(List.of(updatedEvent));

        return eventMapper.toEventFullDto(
                updatedEvent,
                confirmedRequests.getOrDefault(updatedEvent.getId(), 0L),
                views.getOrDefault(updatedEvent.getId(), 0L)
        );
    }

    private void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден.");
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден."));
    }

    private Category findCategoryById(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена."));
    }

    private void updateEventFields(Event event, Object updateDto) {
        String annotation = null;
        Long categoryId = null;
        String description = null;
        LocalDateTime eventDate = null;
        LocationDto locationDto = null;
        Boolean paid = null;
        Integer participantLimit = null;
        Boolean requestModeration = null;
        String title = null;

        if (updateDto instanceof UpdateEventUserRequest userDto) {
            annotation = userDto.getAnnotation();
            categoryId = userDto.getCategory();
            description = userDto.getDescription();
            eventDate = userDto.getEventDate();
            locationDto = userDto.getLocation();
            paid = userDto.getPaid();
            participantLimit = userDto.getParticipantLimit();
            requestModeration = userDto.getRequestModeration();
            title = userDto.getTitle();
        } else if (updateDto instanceof UpdateEventAdminRequest adminDto) {
            annotation = adminDto.getAnnotation();
            categoryId = adminDto.getCategory();
            description = adminDto.getDescription();
            eventDate = adminDto.getEventDate();
            locationDto = adminDto.getLocation();
            paid = adminDto.getPaid();
            participantLimit = adminDto.getParticipantLimit();
            requestModeration = adminDto.getRequestModeration();
            title = adminDto.getTitle();
        }

        if (annotation != null) event.setAnnotation(annotation);
        if (categoryId != null) event.setCategory(findCategoryById(categoryId));
        if (description != null) event.setDescription(description);
        if (eventDate != null) {
            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BadRequestException("Дата события должна быть не ранее, чем через 2 часа от текущего момента.");
            }
            event.setEventDate(eventDate);
        }
        if (locationDto != null) {
            event.getLocation().setLat(locationDto.getLat());
            event.getLocation().setLon(locationDto.getLon());
        }
        if (paid != null) event.setPaid(paid);
        if (participantLimit != null) event.setParticipantLimit(participantLimit);
        if (requestModeration != null) event.setRequestModeration(requestModeration);
        if (title != null) event.setTitle(title);
    }

    private void sendHit(HttpServletRequest request) {
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();
        log.info("Отправка статистики: {}", hitDto);
        statsClient.addHit(hitDto);
    }

    Map<Long, Long> getViewsForEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        if (uris.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        ResponseEntity<List<ViewStatsDto>> response;
        try {
            response = statsClient.getStats(start, LocalDateTime.now(), uris, true);
        } catch (Exception e) {
            log.warn("Сервис статистики недоступен. Ошибка: {}", e.getMessage());
            return Collections.emptyMap();
        }

        List<ViewStatsDto> stats = response.getBody();
        if (stats == null || stats.isEmpty()) {
            return Collections.emptyMap();
        }

        return stats.stream()
                .collect(Collectors.toMap(
                        stat -> Long.parseLong(stat.getUri().substring("/events/".length())),
                        ViewStatsDto::getHits
                ));
    }

    Map<Long, Long> getConfirmedRequestsForEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        return requestRepository.countConfirmedRequestsForEvents(eventIds).stream()
                .collect(Collectors.toMap(ConfirmedRequests::getEventId, ConfirmedRequests::getCount));
    }
}