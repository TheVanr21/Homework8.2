package ru.netology.data;

import com.github.javafaker.Faker;
import lombok.Value;
import ru.netology.mode.User;

public class DataHelper {

    @Value
    public static class DataBase {
        private String url;
        private String login;
        private String password;
    }

    private static Faker faker = new Faker();

    public static DataBase getDataBaseConnection() {
        return new DataBase("jdbc:mysql://localhost:3306/app", "app", "pass");
    }

    public static User.AuthData getValidUser() {
        return new User.AuthData("vasya", "qwerty123");
    }

    public static int getRandomValue(int min, int max) {
        return faker.number().numberBetween(min, max);
    }
}
