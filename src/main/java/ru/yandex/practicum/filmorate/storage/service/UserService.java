package ru.yandex.practicum.filmorate.storage.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.InternalException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    @Getter
    private final Map<Integer, User> users = new HashMap<>();

    private final Instant instant = Instant.now();
    private final LocalDate instantAsLocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
    private int currentId = 0;

    public Collection<User> findAllUsers() {
        return users.values();
    }

    public User addUser(User user) {
        emailCheck(user);
        loginCheck(user);
        birthdayCheck(user);
        nameCheck(user);
        user.setId(++currentId);

        users.put(user.getId(), user);
        log.info("Добавлен пользователь: {}, {}", user.getId(), user.getName());
        return user;
    }

    public User updateUser(User updUser) {
        idCheck(updUser.getId());
        mapIdCheck(updUser.getId());
        emailCheck(updUser);
        loginCheck(updUser);
        birthdayCheck(updUser);
        nameCheck(updUser);

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

    public void addFriend(int userId, int friendId) {
        idCheck(userId);
        idCheck(friendId);
        mapIdCheck(userId);
        mapIdCheck(friendId);

        User user = users.get(userId);
        User friend = users.get(friendId);

        user.addFriend(friendId);
        friend.addFriend(userId);

        users.put(userId, user);
        users.put(friendId, friend);
        log.info("Пользователю {} добавлен друг {}", userId, friendId);
    }

    public void deleteFriend(int userId, int friendId) {
        idCheck(userId);
        idCheck(friendId);
        mapIdCheck(userId);
        mapIdCheck(friendId);

        User user = users.get(userId);
        User friend = users.get(friendId);

        user.removeFriend(friendId);
        friend.removeFriend(userId);
        users.put(userId, user);
        users.put(friendId, friend);
        log.info("Пользователь {} удалил друга {}", userId, friendId);
    }

    public Set<User> findAllFriends(int userId) {
        User user = users.get(userId);
        log.info("Получение списка друзей пользователя {}", userId);


        if (user == null) {
            log.warn("Пользователь с ID {} не найден", userId);
            throw new ConditionsNotMetException("Пользователь с ID " + userId + " не найден");
        }

        return user.getFriendsList().stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Set<User> findAllMutualFriends(int userId, int otherUserId) {


        User user = users.get(userId);
        User otherUser = users.get(otherUserId);
        if (user == null) {
            log.warn("Пользователь с ID {} не найден", userId);
            return Collections.emptySet();
        }

        if (otherUser == null) {
            log.warn("Пользователь с ID {} не найден", otherUserId);
            return Collections.emptySet();
        }
        Set<User> userFriends = findAllFriends(userId);
        Set<User> otherUserFriends = findAllFriends(otherUserId);
        userFriends.retainAll(otherUserFriends);

        return userFriends;
    }

    public void idCheck(Integer userId) {
        if (userId == null) {
            log.error("Id не указан");
            throw new InternalException("Id должен быть указан");
        }
    }

    public void mapIdCheck(int userId) {
        if (!users.containsKey(userId)) {
            log.error("Пользователя с этим Id не существует: {}", userId);
            throw new ConditionsNotMetException("Пользователя с таким Id не существует: " + userId);
        }
    }

    private void emailCheck(User user) {
        if (user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.error("Электронная почта пустая или не содержит \"@\": {} .", user.getEmail());
            throw new InternalException(
                    "Электронная почта не должна быть пустой и должна содержать символ \"@\"."
            );
        }
    }

    private void loginCheck(User user) {
        if (user.getLogin() == null || user.getLogin().contains(" ") || (user.getLogin().isBlank())) {
            log.error("Логин пустой или содержит пробелы");
            throw new InternalException("Логин не может быть пустым или содержать пробелы.");
        }
    }

    private void birthdayCheck(User user) {
        if (user.getBirthday() != null) {
            LocalDate birthday = LocalDate.parse(user.getBirthday());
            if (birthday.isAfter(instantAsLocalDate)) {
                log.error("День рождения указан в будущем");
                throw new InternalException("День рождения не может быть в будущем.");
            }
        }
    }

    private void nameCheck(User user) {
        if (user.getName() == null) {
            user.setName(user.getLogin());
        }
    }

    public boolean userExists(Integer userId) {
        return users.containsKey(userId);
    }

}
