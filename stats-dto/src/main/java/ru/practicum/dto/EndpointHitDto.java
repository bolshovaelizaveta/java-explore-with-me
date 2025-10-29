package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitDto {

    private Long id;

    @NotBlank(message = "Идентификатор сервиса (app) не должен быть пустым.")
    private String app;

    @NotBlank(message = "URI не должен быть пустым.")
    private String uri;

    @NotBlank(message = "IP-адрес не должен быть пустым.")
    private String ip;

    @NotNull(message = "Временная метка (timestamp) не должна быть пустой.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}