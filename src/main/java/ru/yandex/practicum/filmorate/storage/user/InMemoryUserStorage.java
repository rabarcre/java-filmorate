package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
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
import java.util.Set;

@Component
@RestController
@RequestMapping(value = "/users")
@RequiredArgsConstructor
public class InMemoryUserStorage implements UserStorage {
    private final UserService userService;

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
    public void addFriend(@PathVariable("userId") int userId, @PathVariable("friendId") int friendId) {
        userService.addFriend(userId, friendId);
    }

    @DeleteMapping(value = "/{userId}/friends/{friendId}")
    @Override
    public void deleteFriend(@PathVariable("userId") int userId, @PathVariable("friendId") int friendId) {
        userService.deleteFriend(userId, friendId);
    }

    @GetMapping(value = "/{userId}/friends")
    @Override
    public Set<User> findAllFriends(@PathVariable("userId") int userId) {
        return userService.findAllFriends(userId);
    }

    @GetMapping(value = "/{userId}/friends/common/{otherUserId}")
    @Override
    public Set<User> findAllMutualFriends(@PathVariable("userId") int userId,
                                          @PathVariable("otherUserId") int otherUserId) {
        return userService.findAllMutualFriends(userId, otherUserId);
    }

}
