package org.finos.legend.engine.shared.core.operational.errorManagement;

import org.json.simple.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to hold an error's exception class name and message regex to match upcoming exceptions against
 */
public class ErrorExceptionOutline {
    /**
     * Exception class name
     */
    private final String exceptionName;

    /**
     * Regex pattern to match with the exception message
     */
    private final Pattern exceptionMessage;

    /**
     * Constructor to define an exception outline holding an exception class name and message regex pair
     * @param exceptionOutline is the JSONObject holding the associated exception outline data.
     */
    public ErrorExceptionOutline(JSONObject exceptionOutline) {
        this.exceptionName = exceptionOutline.get("ExceptionName").toString();
        String messageRegex = exceptionOutline.get("MessageRegex").toString();
        this.exceptionMessage = Pattern.compile(messageRegex, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Method to check if an exception name and message match a predefined name and message regex pair
     * @param name is the simple name of the exception
     * @param message is the message included in the exception
     * @return true if the name and message match the predefined pair false otherwise
     */
    public boolean isMatch(String name, String message)
    {
        Matcher matcher = this.exceptionMessage.matcher(message);
        return name.equals(this.exceptionName) && matcher.find();
    }
}