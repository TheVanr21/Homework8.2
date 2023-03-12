package ru.netology.data;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import ru.netology.mode.Card;
import ru.netology.mode.User;

import static io.restassured.RestAssured.given;

public class ApiHelper {

    public static void auth(User user) {

        Response authResponse =
                given()
                        .baseUri("http://localhost:9999")
                        .header("Content-type", "application/json")
                        .body(new User.AuthData(user.getLogin(), user.getPassword()))
                        .when()
                        .post("/api/auth")
                        .then()
                        .statusCode(200)
                        .extract()
                        .response();

        boolean status = authResponse.getStatusLine().contains("OK");
        Assertions.assertTrue(status);
    }

    public static String getAuthToken(User user, String verificationCode) {

        return given()
                .baseUri("http://localhost:9999")
                .header("Content-type", "application/json")
                .body("{\n" +
                        "  \"login\": \"" + user.getLogin() + "\",\n" +
                        "  \"code\": \"" + verificationCode + "\"\n" +
                        "}"
                )
                .when()
                .post("/api/auth/verification")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .path("token");
    }

    public static Card[] getCards(String token) {

        return given()
                .baseUri("http://localhost:9999")
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/cards")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(Card[].class);
    }

    public static void transfer(String token, String fromNumber, String toNumber, int amount){
        given()
                .baseUri("http://localhost:9999")
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer " + token)
                .body("{\n" +
                        "  \"from\": \"" + fromNumber + "\",\n" +
                        "  \"to\": \"" + toNumber + "\",\n" +
                        "  \"amount\": " + amount + "\n" +
                        "}"
                )
                .when()
                .post("/api/transfer")
                .then()
                .statusCode(200);
    }
}
