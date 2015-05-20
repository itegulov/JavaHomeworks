package ru.ifmo.ctddev.itegulov.rmi;

import java.rmi.RemoteException;

/**
 * @author Daniyar Itegulov
 */
public interface Person {
    enum PersonType {
        Local, Remote
    }

    String getFirstName() throws RemoteException;

    String getSecondName() throws RemoteException;

    String getPassportNumber() throws RemoteException;

    PersonType getType() throws RemoteException;
}
