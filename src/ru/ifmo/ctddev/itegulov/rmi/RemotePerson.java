package ru.ifmo.ctddev.itegulov.rmi;

import java.rmi.RemoteException;

/**
 * @author Daniyar Itegulov
 */
public class RemotePerson implements Person {
    private Person realPerson;

    public RemotePerson(Person person) throws RemoteException {
        this.realPerson = new LocalPerson(person);
    }

    public RemotePerson(String firstName, String secondName, String passportNumber) {
        this.realPerson = new LocalPerson(firstName, secondName, passportNumber);
    }

    @Override
    public String getFirstName() throws RemoteException {
        return realPerson.getFirstName();
    }

    @Override
    public String getSecondName() throws RemoteException {
        return realPerson.getSecondName();
    }

    @Override
    public String getPassportNumber() throws RemoteException {
        return realPerson.getPassportNumber();
    }

    @Override
    public PersonType getType() throws RemoteException {
        return PersonType.Remote;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RemotePerson that = (RemotePerson) o;

        return realPerson.equals(that.realPerson);

    }

    @Override
    public int hashCode() {
        return 1783472 + realPerson.hashCode();
    }
}
