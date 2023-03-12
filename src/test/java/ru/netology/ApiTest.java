package ru.netology;

import org.junit.jupiter.api.*;
import ru.netology.data.ApiHelper;
import ru.netology.data.DBHelper;
import ru.netology.data.DataHelper;
import ru.netology.mode.Card;
import ru.netology.mode.User;

class ApiTest {
   DataHelper.DataBase db;
    User.AuthData validUser;

    private void setFullCardNumbers(Card[] cards){
        for (Card card : cards) {
            String cardFullNumber = DBHelper.getCardFullNumberById(db, card.getId());
            card.setNumber(cardFullNumber);
        }
    }

    @BeforeEach
    public void init() {
        db = DataHelper.getDataBaseConnection();
        validUser = DataHelper.getValidUser();
    }

    @AfterAll
    public static void clearDb() {
        DBHelper.clearDataBase(DataHelper.getDataBaseConnection());
    }

    @Test
    @DisplayName("Should successfully login")
    public void shouldAuth() {
        User user = DBHelper.getUserFromBD(db, validUser.getLogin());
        ApiHelper.auth(user);
        String verificationCode = DBHelper.getVerificationCodeFromDB(db, user);
        String token = ApiHelper.getAuthToken(user, verificationCode);
        ApiHelper.getCards(token);
    }

    @Test
    @DisplayName("Should transfer from 1st to 2nd")
    public void shouldTransfer() {
        User user = DBHelper.getUserFromBD(db, validUser.getLogin());
        ApiHelper.auth(user);
        String verificationCode = DBHelper.getVerificationCodeFromDB(db, user);
        String token = ApiHelper.getAuthToken(user, verificationCode);
        Card[] cards = ApiHelper.getCards(token);
        Assertions.assertTrue(cards.length >= 2);

        setFullCardNumbers(cards);

        int balanceApiOneBefore = cards[0].getBalance();
        int balanceApiTwoBefore = cards[1].getBalance();
        int balanceDBOneBefore = DBHelper.getCardBalanceFromBD(db, cards[0].getId());
        int balanceDBTwoBefore = DBHelper.getCardBalanceFromBD(db, cards[1].getId());

        Assertions.assertEquals(balanceApiOneBefore, balanceDBOneBefore);
        Assertions.assertEquals(balanceApiTwoBefore, balanceDBTwoBefore);

        int amount = DataHelper.getRandomValue(1, balanceApiOneBefore);

        ApiHelper.transfer(token, cards[0].getNumber(), cards[1].getNumber(), amount);

        cards = ApiHelper.getCards(token);

        int balanceApiOneAfter = cards[0].getBalance();
        int balanceApiTwoAfter = cards[1].getBalance();
        int balanceDBOneAfter = DBHelper.getCardBalanceFromBD(db, cards[0].getId());
        int balanceDBTwoAfter = DBHelper.getCardBalanceFromBD(db, cards[1].getId());

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
        User user = DBHelper.getUserFromBD(db, validUser.getLogin());
        ApiHelper.auth(user);
        String verificationCode = DBHelper.getVerificationCodeFromDB(db, user);
        String token = ApiHelper.getAuthToken(user, verificationCode);
        Card[] cards = ApiHelper.getCards(token);

        setFullCardNumbers(cards);

        Assertions.assertTrue(cards.length >= 2);

        int balanceApiOneBefore = cards[0].getBalance();
        int balanceApiTwoBefore = cards[1].getBalance();
        int balanceDBOneBefore = DBHelper.getCardBalanceFromBD(db, cards[0].getId());
        int balanceDBTwoBefore = DBHelper.getCardBalanceFromBD(db, cards[1].getId());

        Assertions.assertEquals(balanceApiOneBefore, balanceDBOneBefore);
        Assertions.assertEquals(balanceApiTwoBefore, balanceDBTwoBefore);

        int amount = DataHelper.getRandomValue(balanceApiTwoBefore + 1, balanceApiTwoBefore + 1000);

        ApiHelper.transfer(token, cards[1].getNumber(), cards[0].getNumber(), amount);

        cards = ApiHelper.getCards(token);

        int balanceApiTwoAfter = cards[1].getBalance();
        int balanceDBTwoAfter = DBHelper.getCardBalanceFromBD(db, cards[1].getId());

        Assertions.assertTrue(balanceApiTwoAfter >= 0, "balanceApiTwoAfter = " + balanceApiTwoAfter);
        Assertions.assertTrue(balanceDBTwoAfter >= 0, "balanceDBTwoAfter = " + balanceDBTwoAfter);
    }
}
