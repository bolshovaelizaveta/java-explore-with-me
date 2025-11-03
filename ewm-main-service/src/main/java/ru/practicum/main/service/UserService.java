package ru.practicum.main.service;

import ru.practicum.main.dto.NewUserRequest;
import ru.practicum.main.dto.UserDto;

import java.util.List;

public interface UserService {

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    UserDto createUser(NewUserRequest newUserRequest);

    void deleteUser(Long userId);
}