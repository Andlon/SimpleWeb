package org.andlon.simpleweb;

/**
 * Created by Andreas on 31.05.2014.
 */
public class MalformedRequestException extends Exception {
    public MalformedRequestException() { super(); }
    public MalformedRequestException(String message) { super(message); }
    public MalformedRequestException(String message, Throwable cause) { super(message, cause); }
    public MalformedRequestException(Throwable cause) { super(cause); }
}
