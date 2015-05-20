package ru.ifmo.ctddev.itegulov.rmi;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * @author Daniyar Itegulov
 */
public class LocalPerson implements Person, Serializable {
    private static final long serialVersionUID = 178396452936720173L;
    private final String firstName, secondName, passportNumber;

    public LocalPerson(Person other) throws RemoteException {
        if (other == null) {
            throw new NullPointerException();
        }
        this.firstName = other.getFirstName();
        this.secondName = other.getSecondName();
        this.passportNumber = other.getPassportNumber();
    }

    public LocalPerson(String firstName, String secondName, String passportNumber) {
        if (firstName == null || secondName == null || passportNumber == null) {
            throw new NullPointerException();
        }
        this.firstName = firstName;
        this.secondName = secondName;
        this.passportNumber = passportNumber;
    }

    @Override
    public String getFirstName() throws RemoteException {
        return firstName;
    }

    @Override
    public String getSecondName() throws RemoteException {
        return secondName;
    }

    @Override
    public String getPassportNumber() throws RemoteException {
        return passportNumber;
    }

    @Override
    public PersonType getType() throws RemoteException {
        return PersonType.Local;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalPerson that = (LocalPerson) o;

        return firstName.equals(that.firstName) &&
                secondName.equals(that.secondName) &&
                passportNumber.equals(that.passportNumber);

    }

    @Override
    public int hashCode() {
        int result = firstName.hashCode();
        result = 31 * result + secondName.hashCode();
        result = 31 * result + passportNumber.hashCode();
        return result;
    }
}
