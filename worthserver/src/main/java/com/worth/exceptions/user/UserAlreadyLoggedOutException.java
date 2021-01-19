package com.worth.exceptions.user;

import com.worth.components.User;

public class UserAlreadyLoggedOutException extends Exception {

    public UserAlreadyLoggedOutException(String s) {
        super(s);
    }
}
