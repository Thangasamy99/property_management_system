package com.pcc.workflow.exception;

/** Thrown when a required upstream service (Property/Land) is unreachable or returns an error. */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }
}
