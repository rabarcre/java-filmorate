package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserService {
    private final Map<Long, User> users = new HashMap<>();

    private final Instant instant = Instant.now();
    private final LocalDate instantAsLocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();

    public Collection<User> findAll() {
        return users.values();
    }

    public User add(User user) {

        if (user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.error("Электронная почта пустая или не содержит \"@\": {} .", user.getEmail());
            throw new ConditionsNotMetException(
                    "Электронная почта не должна быть пустой и должна содержать символ \"@\"."
            );
        }

        if (user.getLogin() == null || user.getLogin().contains(" ")) {
            log.error("Логин пустой или содержит пробелы");
            throw new ConditionsNotMetException("Логин не может быть пустым или содержать пробелы.");
        }

        if (user.getBirthday() != null) {
            LocalDate birthday = LocalDate.parse(user.getBirthday());
            if (birthday.isAfter(instantAsLocalDate)) {
                log.error("День рождения указан в будущем");
                throw new ConditionsNotMetException("День рождения не может быть в будущем.");
            }
        }

        if (user.getName() == null) {
            user.setName(user.getLogin());
        }

        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Добавлен пользователь: {}, {}", user.getId(), user.getName());
        return user;
    }

    public User update(User updUser) {

        if (updUser.getId() == null) {
            log.error("Id не указан");
            throw new ConditionsNotMetException("Id должен быть указан");
        }

        if (!updUser.getEmail().contains("@")) {
            log.error("Электронная почта указана неверно");
            throw new ConditionsNotMetException(
                    "Электронная почта не должна быть пустой и должна содержать символ \"@\"."
            );
        }

        if (updUser.getLogin().contains(" ")) {
            log.error("Логин содержит пробелы");
            throw new ConditionsNotMetException("Логин не может содержать пробелы.");
        }

        if (updUser.getBirthday() != null) {
            LocalDate birthday = LocalDate.parse(updUser.getBirthday());
            if (birthday.isAfter(instantAsLocalDate)) {
                log.error("День рождения указан в будущем");
                throw new ConditionsNotMetException("День рождения не может быть в будущем.");
            }
        }

        if (updUser.getName() == null) {
            updUser.setName(updUser.getLogin());
        }

        if (users.containsKey(updUser.getId())) {
            User oldUser = users.get(updUser.getId());
            if (updUser.getEmail() == null) {
                updUser.setEmail(oldUser.getEmail());
            }
            if (updUser.getLogin() == null) {
                updUser.setLogin(oldUser.getLogin());
            }
            if (updUser.getBirthday() == null) {
                updUser.setBirthday(oldUser.getBirthday());
            }
            if (updUser.getName() == null) {
                updUser.setName(oldUser.getName());
            }

            users.put(updUser.getId(), updUser);
            log.info("Обновлён пользователь: {}, {}", oldUser.getId(), oldUser.getName());
            return updUser;
        } else {
            throw new ConditionsNotMetException("Пользователь с таким Id не существует");
        }
    }

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
