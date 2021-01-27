package com.worth.rmi.callback;

import com.worth.chat.Chat;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class ClientEvent extends RemoteObject implements ClientEventInterface {
    private ConcurrentHashMap<String, Boolean> usersList;
    private final HashMap<String, Chat> chats;
    private ExecutorService threadPool;

    /**
     * Creates a ClientEvent instance
     *
     * @param userList the list of all users and their status (online=true/offline=false)
     * @param chats the hash map that saves for each projects the relative chatReader object
     * @param threadPool the chat readers thread pool
     */
    public ClientEvent(
            ConcurrentHashMap<String, Boolean> userList, // shared object
            HashMap<String, Chat> chats, // shared object
            ExecutorService threadPool) // shared object
    {
        this.usersList = userList;
        this.chats = chats;
        this.threadPool = threadPool;
    }

    /**
     * Method called from server, creates new thread that reads new messages for given project
     *
     * @param projectName the project's name
     * @param ip the project's ip
     * @throws RemoteException
     */
    @Override
    public void notifyProjectIp(String projectName, String ip) throws RemoteException {
        synchronized (this.chats) {
            Chat cr = new Chat(ip);

            this.chats.put(projectName, cr);
            this.threadPool.execute(cr);
        }
    }

    /**
     * Method called from server, terminates project relative thread for chat reading
     *
     * @param projectName the deleted project's name
     * @throws RemoteException
     */
    @Override
    public void notifyDeletedProject(String projectName) throws RemoteException {
        synchronized (this.chats) {
            Chat chat = this.chats.get(projectName);
            chat.stop();
        }
    }

    /**
     * Method called from server, sets the status for given user
     *
     * @param userName the user's username
     * @param status the new status (true=online/false=offline)
     * @throws RemoteException
     */
    @Override
    public void notifyUserEvent(String userName, boolean status) throws RemoteException {
        // atomically replaces old value mapped
        // with given username with the new value
        // or adds a new mapping if given user is a new user
        this.usersList.put(userName, status);
    }
}
