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

import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler.MATCHING_METHODS;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
     * List of regexes - if an exception includes any such keyword in its class name or message the category is a match
     */
    private final ArrayList<Pattern> keywords;

    /**
     * List of error types (essentially sub-categories of errors) associated with this category
     */
    private final ArrayList<ErrorType> errorTypes;

    /**
     * Constructor to read a JSONObject and extract aforementioned data into its correct fields
     * @param errorCategory is the JSONObject storing the data relating to this particular category
     */
    public ErrorCategory(JSONObject errorCategory)
    {
        this.friendlyName = errorCategory.get("CategoryName").toString();

        JSONArray regexKeywords = ((JSONArray) errorCategory.get("Keywords"));
        this.keywords = new ArrayList<>();
        for (Object regexKeyword : regexKeywords)
        {
            this.keywords.add(Pattern.compile(regexKeyword.toString(), Pattern.CASE_INSENSITIVE));
        }
        this.errorTypes = new ArrayList<>();
        for (Object typeObject : (JSONArray) errorCategory.get("Types"))
        {
            ErrorType type = new ErrorType((JSONObject) typeObject);
            this.errorTypes.add(type);
        }
    }

    /**
     * Method to check if an error category and an occurred exception are a match under a specified matching method
     * @param exception is the error that occurred during execution
     * @param method is the type of exception matching we would like to execute
     * @return true if the exception and category are a match false otherwise.
     */
    public boolean match(Exception exception, MATCHING_METHODS method)
    {
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        String name = exception.getClass().getSimpleName();
        if (method == MATCHING_METHODS.ExceptionOutlineMatching)
        {
            return hasMatchingExceptionOutline(name, message);
        }
        else if (method == MATCHING_METHODS.KeywordsMatching)
        {
            return hasMatchingKeywords(name, message);
        }
        else if (method == MATCHING_METHODS.TypeNameMatching)
        {
            return hasMatchingTypeName(name);
        }
        return hasMatchingExceptionOutline(name, message) || hasMatchingKeywords(name, message) || hasMatchingTypeName(name);
    }

    /**
     * check if the exception's message or class name matches any category keywords
     * @param name is the exception's name
     * @param message is the exception's message
     * @return true if the category is a match, false otherwise
     */
    private boolean hasMatchingKeywords(String name, String message)
    {
        return this.keywords.stream().anyMatch(keyword -> keyword.matcher(message).find() || keyword.matcher(name).find());
    }

    /**
     * check if the exception's name and message match any of the defined exception name and message pairs
     * @param name is the exception's name
     * @param message is the exception's message
     * @return true if the category is a match, false otherwise
     */
    private boolean hasMatchingExceptionOutline(String name, String message)
    {
        return this.errorTypes.stream().anyMatch(type -> type.hasMatchingExceptionOutline(name, message));
    }

    /**
     * check if the exception's class name matches any of this category's Types' exception name regex
     * @param name is the exception's name
     * @return true if the category is a match, false otherwise
     */
    private boolean hasMatchingTypeName(String name)
    {
        return this.errorTypes.stream().anyMatch(type -> type.hasMatchingTypeName(name));
    }

    /**
     * @return user-friendly string corresponding to this error category
     */
    public String getFriendlyName()
    {
        return friendlyName;
    }

    /**
     * Class representing an error type which is essentially a sub-category of errors
     */
    private static class ErrorType
    {
        /**
         * more technical but still user-friendly name of the error type
         */
        private final String typeName;

        /**
         * Regex to match against an unknown exception's class name
         */
        private final Pattern typeExceptionRegex;

        /**
         * List of exception outlines (exception class name and message regex pairs) associated with the Type.
         */
        private final ArrayList<ErrorExceptionOutline> exceptionOutlines;

        /**
         * Constructor that reads the Type data and initialises the ErrorExceptionOutlines and populates aforementioned fields
         * @param type is the JSON data specific to this error type.
         */
        public ErrorType(JSONObject type)
        {
            this.typeName = type.get("TypeName").toString();
            this.typeExceptionRegex = Pattern.compile(type.get("TypeExceptionRegex").toString(), Pattern.CASE_INSENSITIVE);

            this.exceptionOutlines = new ArrayList();
            for (Object jsonException : (JSONArray) type.get("Exceptions"))
            {
                ErrorExceptionOutline exceptionOutline = new ErrorExceptionOutline((JSONObject) jsonException);
                this.exceptionOutlines.add(exceptionOutline);
            }
        }

        /**
         * Checks if an occurring exception class name and message match any exception outline associated with this type.
         * @param name is the exception class name
         * @param message is the exception message
         * @return true if the exception matches an outline class name and message regex pair and false otherwise.
         */
        public boolean hasMatchingExceptionOutline(String name, String message)
        {
            return this.exceptionOutlines.stream().anyMatch(exceptionOutline -> exceptionOutline.matches(name, message));
        }

        /**
         * Method to check if an occurring exception class name matches the defined exception class name regex for this error type
         * @param name is the occurring exception class name
         * @return true if the type name regex matches the parameter name and false otherwise
         */
        public boolean hasMatchingTypeName(String name)
        {
            Matcher matcher = this.typeExceptionRegex.matcher(name);
            return !this.typeExceptionRegex.toString().equals("") && matcher.find();
        }

        /**
         * Method to get the user-friendly but more technical error Type name
         * @return error type friendly name
         */
        public String getTypeName()
        {
            return typeName;
        }


        /**
         * Class to hold an error's exception class name and message regex to match upcoming exceptions against
         */
        private static class ErrorExceptionOutline
        {
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
            public ErrorExceptionOutline(JSONObject exceptionOutline)
            {
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
            public boolean matches(String name, String message)
            {
                Matcher matcher = this.exceptionMessage.matcher(message);
                return name.equals(this.exceptionName) && matcher.find();
            }
        }
    }
}
