package ru.yandex.practicum.filmorate.DAO.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserDAO {
    private final JdbcTemplate jdbcTemplate;

    private final Instant instant = Instant.now();
    private final LocalDate instantAsLocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();

    public Collection<User> findAllUsers() {
        String query = "SELECT * FROM USERS";
        return jdbcTemplate.query(query, this::mapToUser);
    }

    public User addUser(User user) {
        validateUser(user);
        String query = "INSERT INTO USERS (NAME, LOGIN, EMAIL, BIRTHDAY) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, user.getName());
            preparedStatement.setString(2, user.getLogin());
            preparedStatement.setString(3, user.getEmail());
            LocalDate birthday = LocalDate.parse(user.getBirthday());
            preparedStatement.setDate(4, birthday != null ? Date.valueOf(birthday) : null);
            return preparedStatement;
        }, keyHolder);
        user.setId(keyHolder.getKey().intValue());
        return user;
    }

    public User updateUser(User user) {
        if (user.getId() == null) {
            throw new ValidationException("ID пользователя обязателен при обновлении.");
        }
        validateUser(user);

        if (!userExistsDb(user.getId())) {
            throw new ConditionsNotMetException("Пользователь с ID " + user.getId() + " не найден.");
        }

        String query = "UPDATE USERS SET NAME = ?, LOGIN = ?, EMAIL = ?, BIRTHDAY = ? WHERE USER_ID = ?";
        LocalDate birthday = LocalDate.parse(user.getBirthday());
        jdbcTemplate.update(query,
                user.getName(),
                user.getLogin(),
                user.getEmail(),
                birthday,
                user.getId());
        return user;
    }

    public void addFriend(Integer userId, Integer friendId) {
        userExists(userId);
        userExists(friendId);
        if (friendshipExists(userId, friendId)) {
            log.error("Дружба между {} и {} уже существует", userId, friendId);
            throw new IllegalArgumentException("Дружба между " + userId + " и " + friendId + " уже существует");
        }

        String query = "INSERT INTO FRIENDSHIPS (USER_ID, FRIEND_ID, STATUS) VALUES (?, ?, 'CONFIRMED')";
        try {
            jdbcTemplate.update(query, userId, friendId);
            log.info("Пользователь {} теперь дружит с {}", userId, friendId);
        } catch (DataAccessException e) {
            log.error("Ошибка при добавлении дружбы между {} и {}: {}", userId, friendId, e.getMessage());
            throw new RuntimeException("Не удалось добавить дружбу", e);
        }
    }

    public void deleteFriend(Integer userId, Integer friendId) {
        userExists(userId);
        userExists(friendId);

        String query = "DELETE FROM FRIENDSHIPS WHERE USER_ID = ? AND FRIEND_ID = ?";

        int result = jdbcTemplate.update(query, userId, friendId);
        if (result == 0) {
            log.error("Дружба между {} и {} не существует", userId, friendId);
        }
        log.info("Пользователь {} удалил из друзей {}", userId, friendId);
    }

    public Set<User> getAllFriends(Integer userId) {
        userExists(userId);
        try {
            log.info("Получение списка друзей пользователя {}", userId);

            String query = "SELECT u.* " +
                    "FROM USERS u " +
                    "JOIN FRIENDSHIPS f ON u.USER_ID = f.FRIEND_ID " +
                    "WHERE f.USER_ID = ? AND f.STATUS = 'CONFIRMED'";

            List<User> friendList = jdbcTemplate.query(query, this::mapToUser, userId);
            log.info("Пользователь {} дружит с {}", userId, String.valueOf(new HashSet<>(friendList)));

            return new HashSet<>(friendList);
        } catch (RuntimeException e) {
            log.error("Ошибка при получении списка друзей для пользователя {}: {}", userId, e.getMessage());
            throw new RuntimeException("Ошибка при получении списка друзей", e);
        }
    }

    public Set<User> getMutualFriends(Integer userId1, Integer userId2) {
        userExists(userId1);
        userExists(userId2);

        String query = "SELECT u.* " +
                "FROM USERS u " +
                "JOIN FRIENDSHIPS f1 ON u.USER_ID = f1.FRIEND_ID " +
                "JOIN FRIENDSHIPS f2 ON u.USER_ID = f2.FRIEND_ID " +
                "WHERE f1.USER_ID = ? AND f2.USER_ID = ? AND f1.STATUS = 'CONFIRMED' AND f2.STATUS = 'CONFIRMED'";

        List<User> mutualFriendsList = jdbcTemplate.query(query, this::mapToUser, userId1, userId2);
        return new HashSet<>(mutualFriendsList);
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ValidationException("Email не может быть пустым.");
        }
        if (!user.getEmail().contains("@")) {
            log.error("Электронная почта не содержит \"@\": {} .", user.getEmail());
            throw new ValidationException("Электронная почта должна содержать символ \"@\".");
        }
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new ValidationException("Login не может быть пустым.");
        }

        if (user.getBirthday() != null) {
            LocalDate birthday = LocalDate.parse(user.getBirthday());
            if (birthday.isAfter(instantAsLocalDate)) {
                log.error("День рождения указан в будущем");
                throw new ValidationException("День рождения не может быть в будущем.");
            }
        }
    }

    boolean userExistsDb(Integer id) {
        String query = "SELECT COUNT(*) FROM USERS WHERE USER_ID = ?";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, id);
        return (count != null && count > 0);
    }

    public void userExists(Integer userId) {
        if (!userExistsDb(userId)) {
            throw new ConditionsNotMetException("Пользователя с Id " + userId + " не существует");
        }
    }

    private boolean friendshipExists(Integer userId, Integer friendId) {
        String query = "SELECT COUNT(*) FROM FRIENDSHIPS WHERE USER_ID = ? AND FRIEND_ID = ?";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, userId, friendId);
        return count != null && count > 0;
    }

    private User mapToUser(ResultSet resultSet, int rowNum) throws SQLException {
        User user = new User();

        user.setId(resultSet.getInt("USER_ID"));
        user.setLogin(resultSet.getString("LOGIN"));
        user.setEmail(resultSet.getString("EMAIL"));
        user.setBirthday(resultSet.getString("BIRTHDAY"));

        return user;
    }
}
