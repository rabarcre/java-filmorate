package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface UserStorage {
    Collection<User> findAllUsers();

    User addUser(@RequestBody User user);

    User updateUser(@RequestBody User updUser);

    void addFriend(Long userId, Long friendId);

    void deleteFriend(Long userId, Long friendId);

    List<User> findAllFriends(Long userId);

    List<User> findAllMutualFriends(Long userId, Long otherUserId);
}
