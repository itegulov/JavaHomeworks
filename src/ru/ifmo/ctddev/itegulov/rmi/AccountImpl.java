package ru.ifmo.ctddev.itegulov.rmi;

import java.rmi.RemoteException;

/**
 * @author Daniyar Itegulov
 */
public class AccountImpl implements Account {
    private long balance = 0;
    private String id;

    public AccountImpl(String id) {
        this.id = id;
    }

    @Override
    public long getBalance() throws RemoteException {
        return balance;
    }

    @Override
    public void setBalance(long balance) throws RemoteException {
        this.balance = balance;
    }

    @Override
    public long increaseBalance(long delta) throws RemoteException {
        return balance += delta;
    }

    @Override
    public String getId() throws RemoteException {
        return id;
    }
}
