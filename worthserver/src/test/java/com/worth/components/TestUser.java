package com.worth.components;

import com.worth.utils.Security;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;

import static org.junit.Assert.*;

public class TestUser {

    @Test
    public void testGetUserName() {
        String username = "test";
        String password = "test";

        User u = new User(username, password);

        assertEquals(username, u.getUserName());
    }

    @Test
    public void testGetPassword() {
        String username = "test";
        String password = "test";
        MessageDigest md = null;

        User u = new User(username, password);

        assertEquals(Security.getSHA256(password), u.getPassword());
    }

    @Test
    public void testGetStatus() {
        String username = "test";
        String password = "test";

        User u = new User(username, password);

        assertFalse(u.isOnline());
    }

    @Test
    public void testSetStatus() {
        String username = "test";
        String password = "test";

        User u = new User(username, password);
        u.setStatus(true);

        assertTrue(u.isOnline());
    }

    @Test
    public void testCheckPassword() {
        String username = "test";
        String password = "test";
        String wrongPassword = "test1";

        User u = new User(username, password);

        assertTrue(u.checkPassword(password));
        assertFalse(u.checkPassword(wrongPassword));
    }

}
