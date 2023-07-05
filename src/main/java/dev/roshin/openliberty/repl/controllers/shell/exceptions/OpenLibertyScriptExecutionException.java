package dev.roshin.openliberty.repl.controllers.shell.exceptions;

public class OpenLibertyScriptExecutionException extends Exception {
    public OpenLibertyScriptExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpenLibertyScriptExecutionException(String message) {
        super(message);
    }
}
