package com.worth.exceptions.card;

import com.sun.jdi.IntegerValue;

import java.net.InetAddress;

public class InvalidCardStateException extends Exception {
    public InvalidCardStateException(String s) {
        super(s);
    }
}
