package ru.yandex.practicum.filmorate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ControllerAdvice("ru.yandex.practicum.filmorate")
public class ErrorHandler {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler
    public Map<String, String> handleValidationException(final ValidationException e) {
        return Map.of("Ошибка в валидации данных.", e.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler
    public Map<String, String> handleConditionsNotMetException(final ConditionsNotMetException e) {
        return Map.of("Искомый объект не найден.", e.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler
    public Map<String, String> unexpectedError(final Throwable e) {
        return Map.of("Произошла непредвиденная ошибка", e.getMessage());
    }
}
