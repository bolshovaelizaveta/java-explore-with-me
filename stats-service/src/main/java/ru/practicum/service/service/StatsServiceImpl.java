package ru.practicum.service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.service.mapper.HitMapper;
import ru.practicum.service.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;
    private final HitMapper hitMapper;

    @Override
    @Transactional
    public void addHit(EndpointHitDto endpointHitDto) {
        statsRepository.save(hitMapper.toEndpointHit(endpointHitDto));
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        if (uris == null || uris.isEmpty()) {
            return unique ?
                    statsRepository.findUniqueStatsWithoutUris(start, end) :
                    statsRepository.findNonUniqueStatsWithoutUris(start, end);
        } else {
            return unique ?
                    statsRepository.findUniqueStats(start, end, uris) :
                    statsRepository.findNonUniqueStats(start, end, uris);
        }
    }
}