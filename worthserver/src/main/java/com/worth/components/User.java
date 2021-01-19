package com.worth.components;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.worth.utils.Security;

/**
 * User implements a user registered to worth.
 */
@JsonIgnoreProperties({"status"})
public class User {
    private String userName;
    private String password;
    private boolean status;

    /**
     * Creates the User object with specified userName and the hash of given password
     *
     * @param userName the unique username of user
     * @param password the plaintext password used by the user to authenticate himself
     */
    public User(String userName, String password) {
        this.userName = userName;
        this.password = Security.getSHA256(password);
        this.status = false;
    }

    /**
     * Used for serialization/deserialization of User instances
     */
    public User() {
        this.status = false;
    }

    /**
     * Returns the userName of this user
     *
     * @return the username of this user
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * Returns the hash of the password of this user
     *
     * @return the hash of the password of this user
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Returns the state of the user (online/offline)
     *
     * @return true if this user is online, false otherwise
     */
    public boolean getStatus() {
        return this.status;
    }

    /**
     * Sets the user name
     *
     * @param userName the user name to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Sets the sha-256 hash of user password
     *
     * @param password the password's hash in bytes
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the status of the user (online/offline)
     *
     * @param onlineStatus the new status of the user (true=online, false=offline)
     */
    public void setStatus(boolean onlineStatus) {
        this.status = onlineStatus;
    }

    /**
     * Checks if given password is the same as user's password
     *
     * @param password the password to check
     * @return true if the password is the same as user's password, false otherwise
     */
    public Boolean checkPassword(String password) {
        return this.password.equals(Security.getSHA256(password));
    }

}
