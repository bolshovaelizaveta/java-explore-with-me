package ru.practicum.main.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewCategoryDto {

    @NotBlank(message = "Имя категории не может быть пустым.")
    @Size(min = 1, max = 50, message = "Имя категории должно содержать от 1 до 50 символов.")
    private String name;
}