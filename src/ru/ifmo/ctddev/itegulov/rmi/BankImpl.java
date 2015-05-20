package ru.ifmo.ctddev.itegulov.rmi;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Daniyar Itegulov
 */
public class BankImpl implements Bank {
    private Map<Person, Map<String, Account>> data = new HashMap<>();
    private Map<String, List<Person>> personByPassport = new HashMap<>();

    @Override
    public void addAccount(Person person, String accountId) throws RemoteException {
        Person newPerson;
        if (person.getType() == Person.PersonType.Local) {
            newPerson = new LocalPerson(person);
        } else {
            newPerson = new RemotePerson(person);
        }
        data.putIfAbsent(newPerson, new HashMap<>());
        personByPassport.putIfAbsent(newPerson.getPassportNumber(), new ArrayList<>());
        personByPassport.get(newPerson.getPassportNumber()).add(newPerson);
        data.get(newPerson).putIfAbsent(accountId, new AccountImpl(accountId));
    }

    @Override
    public Person searchPersonByPassport(String passportNumber, Person.PersonType type) throws RemoteException {
        for (Person person : personByPassport.get(passportNumber)) {
            if (person.getType() == type) {
                return person;
            }
        }
        return null;
    }

    @Override
    public List<Account> getAccounts(Person person) throws RemoteException {
        return data.containsKey(person) ? new ArrayList<>(data.get(person).values()) : null;
    }

    @Override
    public Long getBalance(Person person, String accountId) throws RemoteException {

        return null;
    }
}
