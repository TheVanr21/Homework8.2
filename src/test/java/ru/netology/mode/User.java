package ru.netology.mode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String login;
    private String password;
    private String status;

    @Value
    public static class AuthData {
        String login;
        String password;
    }
}
