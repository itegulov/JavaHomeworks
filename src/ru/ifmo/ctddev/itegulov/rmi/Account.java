package ru.ifmo.ctddev.itegulov.rmi;

import java.rmi.RemoteException;

/**
 * @author Daniyar Itegulov
 */
public interface Account {
    long getBalance() throws RemoteException;

    void setBalance(long balance) throws RemoteException;

    long increaseBalance(long delta) throws RemoteException;

    String getId() throws RemoteException;
}
