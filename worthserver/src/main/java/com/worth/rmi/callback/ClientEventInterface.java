package com.worth.rmi.callback;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientEventInterface extends Remote {

    /* notifies to the client a project chat ip */
    void notifyProjectIp(String projectName, String ip) throws RemoteException;

    /* notifies to the client the deleted project */
    void notifyDeletedProject(String projectName) throws RemoteException;

    /* notifies to the change of a user state (online/offline) */
    void notifyUserEvent(String userName, boolean status) throws RemoteException;
}
