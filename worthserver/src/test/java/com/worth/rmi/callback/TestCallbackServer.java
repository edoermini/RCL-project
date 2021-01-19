package com.worth.rmi.callback;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.junit.Assert.*;

public class TestCallbackServer {

    @BeforeAll
    static void initializeRegistry() throws RemoteException{
        LocateRegistry.createRegistry(6600);
    }

    @Test
    public void testRegisterForEvents() throws RemoteException, AlreadyBoundException, NotBoundException {

        // setting up rmi server side
        CallbackServer cs = new CallbackServer();
        CallbackServerInterface stub = (CallbackServerInterface) UnicastRemoteObject.exportObject(cs, 0);

        String name = "server1";

        Registry reg = LocateRegistry.getRegistry(6600);
        reg.bind(name, stub);


        // setting up client

        class ClientImpl extends RemoteObject implements ClientEventInterface {
            public boolean userEvent = false;
            public boolean projEvent = false;
            public boolean projDel = false;

            public void notifyUserEvent(String userName, boolean status) {userEvent = true;}
            public void notifyProjectIp(String projectName, String ip) {projEvent = true;}
            public void notifyDeletedProject(String projectName) {projEvent = true;}
        }

        CallbackServerInterface server = (CallbackServerInterface) reg.lookup(name);
        ClientEventInterface callbackObj = new ClientImpl();

        ClientEventInterface clientStub = (ClientEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
        server.registerForEvents("user", clientStub);

        Assertions.assertDoesNotThrow(() -> {
            cs.notifyProjectIp("user", "test", "0.0.0.0");
        });

        server.unregisterForEvents("user");
    }

    @Test
    public void testUnregisterForEvents() throws RemoteException, AlreadyBoundException, NotBoundException {

        // setting up rmi server side
        CallbackServer cs = new CallbackServer();
        CallbackServerInterface stub = (CallbackServerInterface) UnicastRemoteObject.exportObject(cs, 39000);

        String name = "server";

        Registry reg = LocateRegistry.getRegistry(6600);
        reg.bind(name, stub);


        // setting up client

        class ClientImpl extends RemoteObject implements ClientEventInterface {
            public boolean userEvent = false;
            public boolean projEvent = false;
            public boolean projDel = false;

            public void notifyUserEvent(String userName, boolean status) {userEvent = true;}
            public void notifyProjectIp(String projectName, String ip) {projEvent = true;}
            public void notifyDeletedProject(String projectName) {projEvent = true;}
        }

        CallbackServerInterface server = (CallbackServerInterface) reg.lookup(name);
        ClientEventInterface callbackObj = new ClientImpl();

        ClientEventInterface clientStub = (ClientEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
        server.registerForEvents("user", clientStub);
        server.unregisterForEvents("user");

        cs.notifyProjectIp("user", "test", "0.0.0.0");

        assertEquals(false, ((ClientImpl) callbackObj).projEvent);
    }
}
