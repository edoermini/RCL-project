package com.worth.rmi.callback;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of callback interface serverside CallbackServerInterface
 */
public class CallbackServer extends RemoteObject implements CallbackServerInterface {
    private final ConcurrentHashMap<String, ClientEventInterface> subscribedClients;

    public CallbackServer() {
        subscribedClients = new ConcurrentHashMap<>();
    }

    /**
     * Method used by clients, adds the calling client into
     * interested in notifications clients list
     *
     * @param userName the username of calling client
     * @param client the event interface of the client
     * @throws RemoteException
     */
    public void registerForEvents(String userName, ClientEventInterface client)
            throws RemoteException {

        this.subscribedClients.put(userName, client);
    }

    /**
     * Method used by clients, removes the calling client from
     * interested in notifications clients list
     *
     * @param userName the username of calling client
     * @throws RemoteException
     */
    public void unregisterForEvents(String userName)
            throws RemoteException {

        this.subscribedClients.remove(userName);
    }

    /**
     * Method used by server, notifies to given client the new project and it's ip
     *
     * @param userName the client's username to send notification
     * @param projectName the project's name to notify
     * @param ip the project's ip to notify
     * @throws RemoteException
     */
    public synchronized void notifyProjectIp(String userName, String projectName, String ip) throws RemoteException {
        if (!this.subscribedClients.containsKey(userName)) {
            return;
        }

        this.subscribedClients.get(userName).notifyProjectIp(projectName, ip);
    }

    /**
     * Method used by server, notifies to deleted project's members the deletion of project
     *
     * @param users the clients usernames list
     * @param projectName the project's name to notify
     * @throws RemoteException
     */
    public synchronized  void notifyDeletedProject(List<String> users, String projectName) throws RemoteException {

        for (String user : users) {
            if (this.subscribedClients.containsKey(user)) {
                this.subscribedClients.get(user).notifyDeletedProject(projectName);
            }
        }
    }

    /**
     * Method used by server, notifies to all clients the new state of given user
     *
     * @param userName the user with new state
     * @param status the new state
     * @throws RemoteException
     */
    public synchronized void newUserStateEvent(String userName, boolean status) throws RemoteException {

        for (ClientEventInterface client : this.subscribedClients.values()) {
            client.notifyUserEvent(userName, status);
        }
    }

}
