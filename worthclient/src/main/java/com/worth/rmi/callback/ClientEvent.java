package com.worth.rmi.callback;

import com.worth.chat.Chat;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientEvent extends RemoteObject implements ClientEventInterface {
    private ConcurrentHashMap<String, Boolean> usersList;
    private ConcurrentHashMap<String, Chat> chats;
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
            ConcurrentHashMap<String, Chat> chats, // shared object
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

        Chat cr = new Chat(ip);
        //Thread t = new Thread(cr);
        //t.start();

        this.chats.put(projectName, cr);
        this.threadPool.execute(cr);
        //this.chatThreads.put(projectName, t);
    }

    /**
     * Method called from server, terminates project relative thread for chat reading
     *
     * @param projectName the deleted project's name
     * @throws RemoteException
     */
    @Override
    public void notifyDeletedProject(String projectName) throws RemoteException {
        //Thread t = this.chatThreads.get(projectName);
        Chat chat = this.chats.get(projectName);

        chat.stop();
        /*
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
         */
        //this.chatThreads.remove(projectName);
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
        this.usersList.put(userName, status);
    }
}
