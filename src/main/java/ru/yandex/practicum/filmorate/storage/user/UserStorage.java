package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Set;

public interface UserStorage {
    Collection<User> findAllUsers();

    User addUser(@RequestBody User user);

    User updateUser(@RequestBody User updUser);

    void addFriend(int userId, int friendId);

    void deleteFriend(int userId, int friendId);

    Set<User> findAllFriends(int userId);

    Set<User> findAllMutualFriends(int userId, int otherUserId);
}
