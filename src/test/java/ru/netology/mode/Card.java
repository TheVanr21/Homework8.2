package ru.netology.mode;

public class Card {
    private String id;
    private String number;
    private int balance;

    public Card() {
    }

    public Card(String id, String number, int balance) {
        this.id = id;
        this.number = number;
        this.balance = balance;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }
}
