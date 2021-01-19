package com.worth;

import com.worth.components.Project;
import com.worth.components.User;
import com.worth.io.Reader;
import com.worth.io.Writer;
import com.worth.managers.ProjectsManager;
import com.worth.managers.UsersManager;
import com.worth.rmi.callback.CallbackServer;
import com.worth.rmi.callback.CallbackServerInterface;
import com.worth.rmi.registration.Registration;
import com.worth.rmi.registration.RegistrationService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {

    public static final int servicePort = 6660;
    public static final int registryPort = 6661;

    public static void main(String[] args) throws IOException {

        // creating DB
        Writer.createDB();

        // creating and getting rmi registry
        LocateRegistry.createRegistry(registryPort);
        Registry reg = LocateRegistry.getRegistry(registryPort);

        // creating callback server
        CallbackServer cs = new CallbackServer();
        CallbackServerInterface stubCallback = (CallbackServerInterface) UnicastRemoteObject.exportObject(cs, 0);

        reg.rebind("CALLBACK-SERVICE", stubCallback);


        // creating users manager
        List<User> users = Reader.restoreUsers();
        UsersManager um = new UsersManager(users, cs);


        // creating projects manager
        List<Project> projects = Reader.restoreProjects();
        ProjectsManager pm = new ProjectsManager(projects, cs);


        // creating registration server
        RegistrationService rs = new RegistrationService(um);
        Registration stubRegistration = (Registration) UnicastRemoteObject.exportObject(rs, 0);

        reg.rebind("REGISTRATION-SERVICE", stubRegistration);


        ServerSocket ssock = new ServerSocket(servicePort);

        ExecutorService threadPool = Executors.newCachedThreadPool();

        // dispatching new incoming connections
        while (true) {
            Socket sock = ssock.accept();
            threadPool.execute(new ServerThread(pm, um, sock));
        }
    }

}
