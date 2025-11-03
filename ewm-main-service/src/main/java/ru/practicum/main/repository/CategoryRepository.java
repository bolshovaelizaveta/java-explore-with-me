package ru.practicum.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}