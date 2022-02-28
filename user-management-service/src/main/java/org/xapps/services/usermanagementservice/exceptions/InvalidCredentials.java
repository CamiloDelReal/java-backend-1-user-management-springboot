package org.xapps.services.usermanagementservice.exceptions;

public class InvalidCredentials extends Exception{
    public InvalidCredentials() {
        super();
    }

    public InvalidCredentials(String message) {
        super(message);
    }
}
