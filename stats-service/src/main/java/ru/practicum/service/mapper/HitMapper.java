package ru.practicum.service.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.service.model.EndpointHit;

@Component
public class HitMapper {

    public EndpointHit toEndpointHit(EndpointHitDto endpointHitDto) {
        EndpointHit hit = new EndpointHit();
        hit.setApp(endpointHitDto.getApp());
        hit.setUri(endpointHitDto.getUri());
        hit.setIp(endpointHitDto.getIp());
        hit.setTimestamp(endpointHitDto.getTimestamp());
        return hit;
    }

    public EndpointHitDto toEndpointHitDto(EndpointHit endpointHit) {
        return new EndpointHitDto(
                endpointHit.getId(),
                endpointHit.getApp(),
                endpointHit.getUri(),
                endpointHit.getIp(),
                endpointHit.getTimestamp()
        );
    }
}