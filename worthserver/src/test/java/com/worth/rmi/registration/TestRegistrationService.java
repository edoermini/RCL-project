package com.worth.rmi.registration;

import com.worth.components.User;
import com.worth.managers.UsersManager;
import com.worth.rmi.callback.CallbackServer;
import org.junit.jupiter.api.Test;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestRegistrationService {

    @Test
    public void testRegister() {
        CallbackServer cs = new CallbackServer();

        UsersManager um = new UsersManager(new ArrayList<User>(), cs);
        int port = 6660;

        try {
            // setting up remote method invocation server side

            RegistrationService rs = new RegistrationService(um);
            Registration stub = (Registration) UnicastRemoteObject.exportObject(rs, 0);

            LocateRegistry.createRegistry(port);
            Registry reg = LocateRegistry.getRegistry(port);

            reg.rebind("REGISTRATION", stub);

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // client side

        Registry reg = null;
        Registration serverObject = null;

        try {
            reg = LocateRegistry.getRegistry(port);
            serverObject = (Registration) reg.lookup("REGISTRATION");

            serverObject.register("user", "password");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }


        assertFalse(um.getUsersList().get("user"));
    }
}
