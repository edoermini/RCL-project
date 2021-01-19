package com.worth.rmi.callback;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CallbackServerInterface extends Remote {

    /* registration/unregistration for
     * new project event (when a user is added to a project) and
     * for list users event */
    void registerForEvents(String userName, ClientEventInterface client) throws RemoteException;
    void unregisterForEvents(String userName) throws RemoteException;

}
