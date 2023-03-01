package ru.netology;

import com.github.javafaker.Faker;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.netology.mode.Card;
import ru.netology.mode.User;

import java.sql.Connection;
import java.sql.DriverManager;

import static io.restassured.RestAssured.given;

class ApiTest {

    static String[] db = {"jdbc:mysql://localhost:3306/app", "app", "pass"};
    String getUserQuery = "SELECT * FROM users WHERE login = ?";
    String getAuthCodeQuery = "SELECT code FROM auth_codes WHERE user_id = ? ORDER BY created DESC LIMIT 1";
    String getFullCardNumberQuery = "SELECT number FROM cards WHERE id = ?";
    String getCardBalanceQuery = "SELECT balance_in_kopecks FROM cards WHERE id = ?";


    private User getUserFromDB(String login, String password) {
        QueryRunner runner = new QueryRunner();
        User user = null;
        try (Connection connection = DriverManager.getConnection(db[0], db[1], db[2]);) {
            user = runner.query(connection, getUserQuery, login, new BeanHandler<>(User.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (user != null) {
            user.setPassword(password);
        } else {
            System.out.println("User '" + login + "' not found");
            Assertions.fail();
        }
        return user;
    }

    private String getCodeFromDB(User user) {
        QueryRunner runner = new QueryRunner();
        String verificationCode = null;
        try (Connection connection = DriverManager.getConnection(db[0], db[1], db[2]);) {
            verificationCode = runner.query(connection, getAuthCodeQuery, user.getId(), new ScalarHandler<>());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (verificationCode == null) {
            System.out.println("There is no validation codes for user " + user.getLogin());
            Assertions.fail();
        }

        return verificationCode;
    }

    private String getCardFullNumberById(String id) {
        QueryRunner runner = new QueryRunner();
        String fullNumber = null;
        try (Connection connection = DriverManager.getConnection(db[0], db[1], db[2]);) {
            fullNumber = runner.query(connection, getFullCardNumberQuery, id, new ScalarHandler<>());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (fullNumber == null) {
            System.out.println("There is no card by id = " + id);
            Assertions.fail();
        }

        return fullNumber;
    }

    private int getCardBalanceFromBD(String id) {
        QueryRunner runner = new QueryRunner();
        Integer cardBalance = null;
        try (Connection connection = DriverManager.getConnection(db[0], db[1], db[2]);) {
            cardBalance = runner.query(connection, getCardBalanceQuery, id, new ScalarHandler<>());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (cardBalance == null) {
            System.out.println("There is no card by id = " + id);
            Assertions.fail();
        }

        return cardBalance / 100;
    }

    @AfterAll
    public static void clearDb() {
        String clearCardTransactions =  "DELETE FROM card_transactions";
        String clearCard =  "DELETE FROM cards";
        String clearAuthCodes =  "DELETE FROM auth_codes";
        String clearUsers =  "DELETE FROM users";
        QueryRunner runner = new QueryRunner();
        try (
                Connection connection = DriverManager.getConnection(db[0], db[1], db[2]);
        ) {
            runner.update(connection, clearCardTransactions);
            runner.update(connection, clearCard);
            runner.update(connection, clearAuthCodes);
            runner.update(connection, clearUsers);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Should successfully login")
    public void shouldAuth() {
        User user = getUserFromDB("vasya", "qwerty123");

        Response authResponse =
                given()
                        .baseUri("http://localhost:9999")
                        .header("Content-type", "application/json")
                        .body(new User.AuthData(user))
                        .when()
                        .post("/api/auth")
                        .then()
                        .statusCode(200)
                        .extract()
                        .response();

        boolean status = authResponse.getStatusLine().contains("OK");
        Assertions.assertTrue(status);

        String verificationCode = getCodeFromDB(user);

        String token =
                given()
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

        given()
                .baseUri("http://localhost:9999")
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer " + token)
                .body(new User.AuthData(user))
                .when()
                .get("/api/cards")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("Should transfer from 1st to 2nd")
    public void shouldTransfer() {
        User user = getUserFromDB("vasya", "qwerty123");

        Response authResponse =
                given()
                        .baseUri("http://localhost:9999")
                        .header("Content-type", "application/json")
                        .body(new User.AuthData(user))
                        .when()
                        .post("/api/auth")
                        .then()
                        .statusCode(200)
                        .extract()
                        .response();

        boolean status = authResponse.getStatusLine().contains("OK");
        Assertions.assertTrue(status);

        String verificationCode = getCodeFromDB(user);

        String token =
                given()
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

        Card[] cards =
                given()
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

        for (Card card : cards) {
            String cardFullNumber = getCardFullNumberById(card.getId());
            card.setNumber(cardFullNumber);
        }

        Assertions.assertTrue(cards.length >= 2);

        int balanceApiOneBefore = cards[0].getBalance();
        int balanceApiTwoBefore = cards[1].getBalance();
        int balanceDBOneBefore = getCardBalanceFromBD(cards[0].getId());
        int balanceDBTwoBefore = getCardBalanceFromBD(cards[1].getId());

        Assertions.assertEquals(balanceApiOneBefore, balanceDBOneBefore);
        Assertions.assertEquals(balanceApiTwoBefore, balanceDBTwoBefore);

        Faker faker = new Faker();
        int amount = faker.number().numberBetween(1, balanceApiOneBefore);

        given()
                .baseUri("http://localhost:9999")
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer " + token)
                .body("{\n" +
                        "  \"from\": \"" + cards[0].getNumber() + "\",\n" +
                        "  \"to\": \"" + cards[1].getNumber() + "\",\n" +
                        "  \"amount\": " + amount + "\n" +
                        "}"
                )
                .when()
                .post("/api/transfer")
                .then()
                .statusCode(200);

        cards =
                given()
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

        int balanceApiOneAfter = cards[0].getBalance();
        int balanceApiTwoAfter = cards[1].getBalance();
        int balanceDBOneAfter = getCardBalanceFromBD(cards[0].getId());
        int balanceDBTwoAfter = getCardBalanceFromBD(cards[1].getId());

        int balanceOneExpected = balanceApiOneBefore - amount;
        int balanceTwoExpected = balanceApiTwoBefore + amount;

        Assertions.assertEquals(balanceOneExpected, balanceApiOneAfter);
        Assertions.assertEquals(balanceOneExpected, balanceDBOneAfter);
        Assertions.assertEquals(balanceTwoExpected, balanceApiTwoAfter);
        Assertions.assertEquals(balanceTwoExpected, balanceDBTwoAfter);
    }

    @Test
    @DisplayName("Should not transfer amounts bigger than balance")
    public void shouldNotOverlap() {
        User user = getUserFromDB("vasya", "qwerty123");

        Response authResponse =
                given()
                        .baseUri("http://localhost:9999")
                        .header("Content-type", "application/json")
                        .body(new User.AuthData(user))
                        .when()
                        .post("/api/auth")
                        .then()
                        .statusCode(200)
                        .extract()
                        .response();

        boolean status = authResponse.getStatusLine().contains("OK");
        Assertions.assertTrue(status);

        String verificationCode = getCodeFromDB(user);

        String token =
                given()
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

        Card[] cards =
                given()
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

        for (Card card : cards) {
            String cardFullNumber = getCardFullNumberById(card.getId());
            card.setNumber(cardFullNumber);
        }

        Assertions.assertTrue(cards.length >= 2);

        int balanceApiOneBefore = cards[0].getBalance();
        int balanceApiTwoBefore = cards[1].getBalance();
        int balanceDBOneBefore = getCardBalanceFromBD(cards[0].getId());
        int balanceDBTwoBefore = getCardBalanceFromBD(cards[1].getId());

        Assertions.assertEquals(balanceApiOneBefore, balanceDBOneBefore);
        Assertions.assertEquals(balanceApiTwoBefore, balanceDBTwoBefore);

        Faker faker = new Faker();
        int amount = faker.number().numberBetween(balanceApiTwoBefore + 1, balanceApiTwoBefore + 1000);

        given()
                .baseUri("http://localhost:9999")
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer " + token)
                .body("{\n" +
                        "  \"from\": \"" + cards[1].getNumber() + "\",\n" +
                        "  \"to\": \"" + cards[0].getNumber() + "\",\n" +
                        "  \"amount\": " + amount + "\n" +
                        "}"
                )
                .when()
                .post("/api/transfer")
                .then()
                .statusCode(200);

        cards =
                given()
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

        int balanceApiTwoAfter = cards[1].getBalance();
        System.out.println("balanceApiTwoAfter="+balanceApiTwoAfter);
        int balanceDBTwoAfter = getCardBalanceFromBD(cards[1].getId());
        System.out.println("balanceDBTwoAfter="+balanceDBTwoAfter);

        Assertions.assertTrue(balanceApiTwoAfter >= 0);
        Assertions.assertTrue(balanceDBTwoAfter >= 0);
    }
}
