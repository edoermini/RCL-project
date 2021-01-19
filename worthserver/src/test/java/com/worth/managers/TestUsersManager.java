package com.worth.managers;

import com.worth.components.User;
import com.worth.exceptions.WrongPasswordException;
import com.worth.exceptions.user.UserAlreadyExistsException;
import com.worth.exceptions.user.UserAlreadyLoggedInException;
import com.worth.exceptions.user.UserAlreadyLoggedOutException;
import com.worth.exceptions.user.UserNotFoundException;
import com.worth.io.Writer;
import com.worth.rmi.callback.CallbackServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class TestUsersManager {

    @BeforeEach
    public void createDB() {
        Writer.createDB();
    }

    @Test
    public void testLogin() throws UserAlreadyLoggedInException, UserNotFoundException, WrongPasswordException {
        User u1 = new User("user1", "test");
        User u2 = new User("user2", "test");
        User u3 = new User("user3", "test");

        ArrayList<User> l = new ArrayList<>();
        l.add(u1);
        l.add(u2);
        l.add(u3);

        CallbackServer cs = new CallbackServer();
        UsersManager um = new UsersManager(l, cs);

        // testing login with success
        um.login("user1", "test");
        assertTrue(um.getUsersList().get("user1"));

        //testing login with UserNotFoundException
        Assertions.assertThrows(UserNotFoundException.class, () -> um.login("user4", "test"));

        //testing login with UserAlreadyLoggedInException
        Assertions.assertThrows(UserAlreadyLoggedInException.class, () -> um.login("user1", "test"));

        //testing login with WrongPasswordException
        Assertions.assertThrows(WrongPasswordException.class, () -> um.login("user2", "test1"));
    }

    @Test
    public void testLogout()
            throws UserAlreadyLoggedInException, UserAlreadyLoggedOutException, UserNotFoundException, WrongPasswordException {
        User u1 = new User("user1", "test");
        User u2 = new User("user2", "test");
        User u3 = new User("user3", "test");

        ArrayList<User> l = new ArrayList<>();
        l.add(u1);
        l.add(u2);
        l.add(u3);

        CallbackServer cs = new CallbackServer();

        UsersManager um = new UsersManager(l, cs);

        um.login("user1", "test");

        // testing logout with success
        um.logout("user1");
        assertFalse(um.getUsersList().get("user1"));

        //testing logout with UserNotFoundException
        Assertions.assertThrows(UserNotFoundException.class, () -> um.logout("user4"));

        //testing login with UserAlreadyLoggedInException
        Assertions.assertThrows(UserAlreadyLoggedOutException.class, () -> um.logout("user1"));
    }

    @Test
    public void testRegister() throws UserAlreadyExistsException {
        User u1 = new User("user1", "test");
        User u2 = new User("user2", "test");
        User u3 = new User("user3", "test");

        ArrayList<User> l = new ArrayList<>();
        l.add(u1);
        l.add(u2);
        l.add(u3);

        CallbackServer cs = new CallbackServer();
        UsersManager um = new UsersManager(l, cs);

        String newUserName = "user4";

        //testing registration with success
        um.register(newUserName, "test");
        assertTrue(um.getUsersList().containsKey(newUserName));
        assertFalse(um.getUsersList().get(newUserName));

        //testing registration with UserAlreadyExistsException
        Assertions.assertThrows(UserAlreadyExistsException.class, () -> um.register(newUserName, "test"));
    }
}
