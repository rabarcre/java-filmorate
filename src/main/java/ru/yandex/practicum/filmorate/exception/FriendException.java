package ru.yandex.practicum.filmorate.exception;

import org.springframework.dao.DataAccessException;

public class FriendException extends RuntimeException {
    public FriendException(String message, DataAccessException e) {
        super(message);
    }

    public FriendException(String message, RuntimeException e) {
        super(message);
    }
}
