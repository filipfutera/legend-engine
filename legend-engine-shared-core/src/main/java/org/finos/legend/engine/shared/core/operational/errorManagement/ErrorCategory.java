// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


package org.finos.legend.engine.shared.core.operational.errorManagement;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to hold data corresponding to a particular category of errors
 * This data is to be used in categorising an exception occurring during execution
 */
public class ErrorCategory
{
    /**
     * User-friendly string overviewing the error category
     */
    private final String friendlyName;

    /**
     * Map of exception data with
     * Keys being the Exception Types associated with the category
     * Values being a tuple of exception class names and exception message regexes
     */
    private final HashMap<String, List<ExceptionOutline>> exceptionDataMap = new HashMap<>();

    /**
     * List of regexes - if an exception includes any such keyword in its class name or message the category is a match
     */
    private final ArrayList<Pattern> keywords = new ArrayList<>();

    /**
     * Maps Exception Types to general regexes used to match an exception with matching class name to the Type and thus category
     * Different to keywords as only used to match with exception class name rather than also the exception message.
     */
    private final HashMap<String, Pattern> typeRegexMap = new HashMap<>();

    /**
     * Constructor to read a JSONObject and extract aforementioned data into its correct fields
     * @param errorCategory is the JSONObject storing the data relating to this particular category
     */
    public ErrorCategory(JSONObject errorCategory)
    {
        this.friendlyName = errorCategory.get("CategoryName").toString();

        JSONArray regexKeywords = ((JSONArray) errorCategory.get("Keywords"));
        for (Object regexKeyword : regexKeywords)
        {
            this.keywords.add(Pattern.compile(regexKeyword.toString(), Pattern.CASE_INSENSITIVE));
        }

        for (Object typeObject : (JSONArray) errorCategory.get("Types"))
        {
            JSONObject type = (JSONObject) typeObject;
            String typeName = type.get("TypeName").toString();
            Pattern typeExceptionRegex = Pattern.compile(type.get("TypeExceptionRegex").toString(), Pattern.CASE_INSENSITIVE);
            typeRegexMap.put(typeName, typeExceptionRegex);

            ArrayList<ExceptionOutline> exceptions = new ArrayList();
            for (Object jsonException : (JSONArray) type.get("Exceptions"))
            {
                JSONObject exceptionData = (JSONObject) jsonException;
                String exceptionName = exceptionData.get("ExceptionName").toString();
                String exceptionMessageRegex = exceptionData.get("MessageRegex").toString();
                exceptions.add(new ExceptionOutline(exceptionName, Pattern.compile(exceptionMessageRegex, Pattern.CASE_INSENSITIVE)));
            }
            exceptionDataMap.put(typeName, exceptions);
        }
    }

    /**
     * Method to check if an error category and an occurred exception are a match
     * @param exception is the error that occurred during execution
     * @return true if the exception and category are a match false otherwise.
     */
    public boolean match(Exception exception)
    {
        String message = exception.getMessage() == null ? "null" : exception.getMessage();
        String name = exception.getClass().getSimpleName();
        return matchKeywords(name, message) || matchExceptionOutlines(name, message) || matchTypeNames(name);
    }

    /**
     * check if the exception's message or class name matches any category keywords
     * @param name is the exception's name
     * @param message is the exception's message
     * @return true if the category is a match, false otherwise
     */
    private boolean matchKeywords(String name, String message)
    {
        for (Pattern keyword : keywords)
        {
            if (keyword.matcher(message).find() || keyword.matcher(name).find())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * check if the exception's name and message match any of the defined exception name and message pairs
     * @param name is the exception's name
     * @param message is the exception's message
     * @return true if the category is a match, false otherwise
     */
    private boolean matchExceptionOutlines(String name, String message)
    {
        for (Entry<String, List<ExceptionOutline>> entry : exceptionDataMap.entrySet())
        {
            String type = entry.getKey();
            for (ExceptionOutline exceptionData : entry.getValue())
            {
                Matcher matcher = exceptionData.exceptionMessage.matcher(message);
                if (name.equals(exceptionData.exceptionName) && matcher.find())
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * check if the exception's class name matches any of this category's Types' exception name regex
     * @param name is the exception's name
     * @return true if the category is a match, false otherwise
     */
    private boolean matchTypeNames(String name)
    {
        for (Entry<String, Pattern> exceptionRegex : typeRegexMap.entrySet())
        {
            String type = exceptionRegex.getKey();
            Pattern regex = exceptionRegex.getValue();
            Matcher matcher = regex.matcher(name);
            if (!regex.toString().equals("") && matcher.find())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @return user-friendly string corresponding to this error category
     */
    public String getFriendlyName()
    {
        return friendlyName;
    }

    /**
     * Local class to implement a tuple holding exception data to match errors with
     */
    private static class ExceptionOutline
    {
        /**
         * Exception class name
         */
        public final String exceptionName;

        /**
         * Regex pattern to match with the exception message
         */
        public final Pattern exceptionMessage;

        /**
         * Constructor for tuple class
         * @param exceptionName is the exception class name
         * @param exceptionMessage is the exception message regex
         */
        public ExceptionOutline(String exceptionName, Pattern exceptionMessage)
        {
            this.exceptionName = exceptionName;
            this.exceptionMessage = exceptionMessage;
        }
    }
}
