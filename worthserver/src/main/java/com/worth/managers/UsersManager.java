package com.worth.managers;

import com.worth.components.User;
import com.worth.exceptions.WrongPasswordException;
import com.worth.exceptions.user.UserAlreadyExistsException;
import com.worth.exceptions.user.UserAlreadyLoggedInException;
import com.worth.exceptions.user.UserAlreadyLoggedOutException;
import com.worth.exceptions.user.UserNotFoundException;
import com.worth.io.Writer;
import com.worth.rmi.callback.CallbackServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersManager {
    private final HashMap<String, User> onlineUsers;
    private final HashMap<String, User> offlineUsers;
    private final CallbackServer callback;

    public UsersManager(List<User> users, CallbackServer callback) {

        this.onlineUsers = new HashMap<>();
        this.offlineUsers = new HashMap<>();

        for (User u : users) {
            this.offlineUsers.put(u.getUserName(), u);
        }

        this.callback = callback;
    }

    /**
     * Changes user status to online
     *
     * @param userName the user to login
     * @param password the user's password
     * @throws UserAlreadyLoggedInException if user is already online
     * @throws UserNotFoundException if user doesn't exist
     * @throws WrongPasswordException if password is not the same as user's password
     */
    public synchronized void login(String userName, String password)
            throws UserAlreadyLoggedInException, UserNotFoundException, WrongPasswordException {

        if (this.onlineUsers.containsKey(userName)) {
            throw new UserAlreadyLoggedInException("User " + userName + " is already logged in");
        }

        if (!this.offlineUsers.containsKey(userName)) {
            throw new UserNotFoundException("User " + userName + " is not registered");
        }

        if (!this.offlineUsers.get(userName).checkPassword(password)) {
            throw new WrongPasswordException("Wrong password");
        }

        // returns the reference to object mapped with userName key
        User u = this.offlineUsers.remove(userName);
        u.setStatus(true);
        this.onlineUsers.put(u.getUserName(), u);

        // notifying new user state
        this.callback.newUserStateEvent(userName, true);
    }

    /**
     * Changes user status to offline
     *
     * @param userName the user to logout
     * @throws UserAlreadyLoggedOutException if user is already offline
     * @throws UserNotFoundException if user doesn't exist
     */
    public synchronized void logout(String userName) throws UserAlreadyLoggedOutException, UserNotFoundException {

        if (this.offlineUsers.containsKey(userName)) {
            throw new UserAlreadyLoggedOutException("User " + userName + " is already logged out");
        }

        if (!this.onlineUsers.containsKey(userName)) {
            throw new UserNotFoundException("User " + userName + " doesn't exists");
        }

        // returns the reference to object mapped with userName key
        User u = this.onlineUsers.remove(userName);
        u.setStatus(false);
        this.offlineUsers.put(u.getUserName(), u);

        // notifying new user state
        this.callback.newUserStateEvent(userName, false);
    }

    /**
     * Adds a user and sets his status as offline
     *
     * @param userName the user to register
     * @param password the user's password
     * @throws UserAlreadyExistsException if user with given username already exists
     */
    public synchronized void register(String userName, String password) throws UserAlreadyExistsException {

        if (this.offlineUsers.containsKey(userName)) {
            throw new UserAlreadyExistsException("User with username " + userName + " already exists");
        }

        if (this.onlineUsers.containsKey(userName)) {
            throw new UserAlreadyExistsException("User with username " + userName + " already exists");
        }

        User u = new User(userName, password);
        this.offlineUsers.put(userName, u);

        // notifying new user state
        this.callback.newUserStateEvent(userName, false);

        // writing user into filesystem
        Writer.addUser(u);
    }

    /**
     * Returns the list of users and the relative status (true=online, false=offline)
     *
     * @return the list of users and the relative status (true=online, false=offline)
     */
    public synchronized Map<String, Boolean> getUsersList() {
        HashMap<String, Boolean> map = new HashMap<>();

        for (User u : this.onlineUsers.values()) {
            map.put(u.getUserName(), true);
        }

        for (User u : this.offlineUsers.values()) {
            map.put(u.getUserName(), false);
        }

        return map;
    }

    /**
     * Checks if a user with given username exists
     *
     * @param userName the username to check
     * @return true if a user with given username exists, false otherwise
     */
    public synchronized boolean exists(String userName) {
        return this.onlineUsers.containsKey(userName) || this.offlineUsers.containsKey(userName);
    }

}
