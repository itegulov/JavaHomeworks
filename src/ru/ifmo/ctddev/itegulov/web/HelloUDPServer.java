package ru.ifmo.ctddev.itegulov.web;

/**
 * @author Daniyar Itegulov
 */
public class HelloUDPServer {
    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Usage: HelloUDPServer port threadNumber");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
            if (port < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            System.err.println("Port number must be a legal non-negative number");
        }

        int threads;
        try {
            threads = Integer.parseInt(args[1]);
            if (threads <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            System.err.println("Thread number must be a legal positive number");
        }
        
    }
}
