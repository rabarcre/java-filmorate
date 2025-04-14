package ru.yandex.practicum.filmorate.storage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UserService {
    private final Map<Long, User> users = new HashMap<>();

    private final Instant instant = Instant.now();
    private final LocalDate instantAsLocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();

    public Collection<User> findAllUsers() {
        return users.values();
    }

    public User addUser(User user) {
        emailCheck(user);
        loginCheck(user);
        birthdayCheck(user);
        nameCheck(user);
        if (user.getFriendList() == null) {
            List<User> friendList = new ArrayList<>();
            user.setFriendList(friendList);
        }

        user.setId(getNextId());
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

    //добавление в друзья, удаление из друзей, вывод списка общих друзей
    public void addFriend(Long userId, Long friendId) {
        idCheck(userId);
        idCheck(friendId);
        mapIdCheck(userId);
        mapIdCheck(friendId);

        User user = users.get(userId);
        User friend = users.get(friendId);
        List<User> friendList = user.getFriendList();
        List<User> otherFriendList = friend.getFriendList();
        for (User repeatableFriend : friendList) {
            if (friendList.contains(repeatableFriend)) {
                log.error("Друг с таким Id уже добавлен: {}", friendId);
                throw new ValidationException("Друг с таким Id уже добавлен: " + friendId);
            }
        }
        friendList.add(friend);
        user.setFriendList(friendList);

        otherFriendList.add(user);
        friend.setFriendList(otherFriendList);
        users.put(userId, user);
        users.put(friendId, friend);
        log.info("Пользователю {} добавлен друг {}", userId, friendId);
    }

    public void deleteFriend(Long userId, Long friendId) {
        idCheck(userId);
        idCheck(friendId);
        mapIdCheck(userId);
        mapIdCheck(friendId);

        User user = users.get(userId);
        List<User> friendList = user.getFriendList();
        User friend = users.get(friendId);
        List<User> otherFriendList = friend.getFriendList();
        for (User value : friendList) {
            if (value.getId().equals(friendId)) {
                friendList.remove(value);
                user.setFriendList(friendList);
                otherFriendList.remove(value);
                friend.setFriendList(otherFriendList);
                users.put(userId, user);
                log.info("Пользователь {} удалил друга {}", userId, friendId);
            } else {
                log.error("Id друга указан неверно");
                throw new ConditionsNotMetException("Id друга указан неверно или такого друга не существует");
            }
        }
    }

    public List<User> findAllFriends(Long userId) {
        idCheck(userId);
        mapIdCheck(userId);
        User user = users.get(userId);
        log.info("Все друзья для {}: {}", user.getId(), user.getFriendList());
        return user.getFriendList();
    }

    public List<User> findAllMutualFriends(Long userId, Long otherUserId) {
        idCheck(userId);
        idCheck(otherUserId);
        mapIdCheck(userId);
        mapIdCheck(otherUserId);

        User user = users.get(userId);
        User otherUser = users.get(otherUserId);
        List<User> friendList = user.getFriendList();
        List<User> otherFriendList = otherUser.getFriendList();
        List<User> mutualFriendList = new ArrayList<>();

        for (User friend : friendList) {
            if (otherFriendList.contains(friend)) {
                mutualFriendList.add(friend);
                log.info("Общий друг для {} и {} : {}", user.getId(), otherUser.getId(), friend);
            }
        }
        return mutualFriendList;
    }

    public void idCheck(Long userId) {
        if (userId == null) {
            log.error("Id не указан");
            throw new ValidationException("Id должен быть указан");
        }
    }

    public void mapIdCheck(Long userId) {
        if (!users.containsKey(userId)) {
            log.error("Пользователя с этим Id не существует: {}", userId);
            throw new ConditionsNotMetException("Пользователя с таким Id не существует: " + userId);
        }
    }

    private void emailCheck(User user) {
        if (user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.error("Электронная почта пустая или не содержит \"@\": {} .", user.getEmail());
            throw new ValidationException(
                    "Электронная почта не должна быть пустой и должна содержать символ \"@\"."
            );
        }
    }

    private void loginCheck(User user) {
        if (user.getLogin() == null || user.getLogin().contains(" ") || (user.getLogin().isBlank())) {
            log.error("Логин пустой или содержит пробелы");
            throw new ValidationException("Логин не может быть пустым или содержать пробелы.");
        }
    }

    private void birthdayCheck(User user) {
        if (user.getBirthday() != null) {
            LocalDate birthday = LocalDate.parse(user.getBirthday());
            if (birthday.isAfter(instantAsLocalDate)) {
                log.error("День рождения указан в будущем");
                throw new ValidationException("День рождения не может быть в будущем.");
            }
        }
    }

    private void nameCheck(User user) {
        if (user.getName() == null) {
            user.setName(user.getLogin());
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
