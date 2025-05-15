package com.tuneit.jackalope.integration.babelnet.exceptions;

public class IntegrationException extends Exception {
    public IntegrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public IntegrationException(String message) {
        super(message);
    }
}
