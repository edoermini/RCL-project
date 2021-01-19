package com.worth.rmi.registration;

import com.worth.exceptions.user.UserAlreadyExistsException;
import com.worth.managers.UsersManager;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;

/**
 * Implementation of Registration interface
 */
public class RegistrationService extends RemoteServer implements Registration {
    private final UsersManager um; // shared, used to add globally new users

    public RegistrationService(UsersManager um) {
        this.um = um;
    }

    @Override
    public String register(String userName, String password) throws RemoteException {

        if (password.equals("")) {
            return "3%Empty password is not permitted";
        }
        try {
            this.um.register(userName, password);
        } catch (UserAlreadyExistsException e) {
            return "2%" + e.getMessage();
        }

        return "0%User " + userName + " successfully registered";
    }
}
