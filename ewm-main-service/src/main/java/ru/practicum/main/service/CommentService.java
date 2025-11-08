package ru.practicum.main.service;

import ru.practicum.main.dto.CommentDto;
import ru.practicum.main.dto.NewCommentDto;

import java.util.List;

public interface CommentService {
    CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    CommentDto updateComment(Long userId, Long commentId, NewCommentDto newCommentDto);

    void deleteCommentByUser(Long userId, Long commentId);

    List<CommentDto> getCommentsForEvent(Long eventId);

    void deleteCommentByAdmin(Long commentId);
}