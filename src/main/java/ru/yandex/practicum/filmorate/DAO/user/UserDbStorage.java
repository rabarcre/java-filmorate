package ru.yandex.practicum.filmorate.DAO.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.Set;

@Component("UserDbStorage")
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {
    private final UserDAO userDAO;

    @Override
    public Collection<User> findAllUsers() {
        return userDAO.findAllUsers();
    }

    @Override
    public User addUser(User user) {
        return userDAO.addUser(user);
    }

    @Override
    public User updateUser(User updUser) {
        return userDAO.updateUser(updUser);
    }

    @Override
    public void addFriend(int userId, int friendId) {
        userDAO.addFriend(userId, friendId);
    }

    @Override
    public void deleteFriend(int userId, int friendId) {
        userDAO.deleteFriend(userId, friendId);
    }

    @Override
    public Set<User> findAllFriends(int userId) {
        return userDAO.getAllFriends(userId);
    }

    @Override
    public Set<User> findAllMutualFriends(int userId, int otherUserId) {
        return userDAO.getMutualFriends(userId, otherUserId);
    }
}
