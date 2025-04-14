package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.service.UserService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@RestController
@RequestMapping(value = "/users")
public class InMemoryUserStorage implements UserStorage {
    private final UserService userService;

    public InMemoryUserStorage(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Override
    public Collection<User> findAllUsers() {
        return userService.findAllUsers();
    }

    @PostMapping
    @Override
    public User addUser(@RequestBody User user) {
        return userService.addUser(user);
    }

    @PutMapping
    @Override
    public User updateUser(@RequestBody User updUser) {
        return userService.updateUser(updUser);
    }

    @PutMapping(value = "/{userId}/friends/{friendId}")
    @Override
    public void addFriend(@PathVariable("userId") Long userId, @PathVariable("friendId") Long friendId) {
        userService.addFriend(userId, friendId);
    }

    @DeleteMapping(value = "/{userId}/friends/{friendId}")
    @Override
    public void deleteFriend(@PathVariable("userId") Long userId, @PathVariable("friendId") Long friendId) {
        userService.deleteFriend(userId, friendId);
    }

    @GetMapping(value = "/{userId}/friends")
    @Override
    public List<User> findAllFriends(@PathVariable("userId") Long userId) {
        return userService.findAllFriends(userId);
    }

    @GetMapping(value = "/{userId}/friends/common/{otherUserId}")
    @Override
    public List<User> findAllMutualFriends(@PathVariable("userId") Long userId,
                                           @PathVariable("otherUserId") Long otherUserId) {
        return userService.findAllMutualFriends(userId, otherUserId);
    }

}
