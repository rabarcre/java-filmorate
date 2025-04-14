package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.service.UserService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    UserService userService = new UserService();

    @Test
    void shouldThrowExceptionIfEmailIsBlank() {
        User user = new User();
        user.setLogin("login");
        user.setEmail("");
        user.setBirthday("1900-12-12");
        user.setName("name");

        assertThrows(ValidationException.class, () -> {
            userService.addUser(user);
        });
    }

    @Test
    void shouldThrowExceptionIfEmailDoesntHaveSpecialChar() {
        User user = new User();
        user.setLogin("login");
        user.setEmail("botbotkarta.ru");
        user.setBirthday("1900-12-12");
        user.setName("name");

        assertThrows(ValidationException.class, () -> {
            userService.addUser(user);
        });
    }

    @Test
    void shouldThrowExceptionIfLoginIsBlank() {
        User user = new User();
        user.setLogin("");
        user.setEmail("botbot@karta.ru");
        user.setBirthday("1900-12-12");
        user.setName("name");

        assertThrows(ValidationException.class, () -> {
            userService.addUser(user);
        });
    }

    @Test
    void shouldThrowExceptionIfLoginContainsSpace() {
        User user = new User();
        user.setLogin("login log");
        user.setEmail("botbot@karta.ru");
        user.setBirthday("1900-12-12");
        user.setName("name");

        assertThrows(ValidationException.class, () -> {
            userService.addUser(user);
        });
    }

    @Test
    void shouldThrowExceptionIfBirthdayIsInFuture() {
        User user = new User();
        user.setLogin("login");
        user.setEmail("botbot@karta.ru");
        user.setBirthday(LocalDate.now().plusDays(1).toString());
        user.setName("name");

        assertThrows(ValidationException.class, () -> {
            userService.addUser(user);
        });
    }
}