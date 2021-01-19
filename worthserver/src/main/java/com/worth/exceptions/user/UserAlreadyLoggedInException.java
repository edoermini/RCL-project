package com.worth.exceptions.user;

public class UserAlreadyLoggedInException extends Exception {

    public UserAlreadyLoggedInException(String s) {
        super(s);
    }
}
