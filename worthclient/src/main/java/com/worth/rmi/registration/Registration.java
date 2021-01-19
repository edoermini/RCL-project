package com.worth.rmi.registration;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Registration extends Remote {
    String register(String userName, String password) throws RemoteException;
}
