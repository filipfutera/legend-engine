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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler.MatchingMethod;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to hold data corresponding to a particular category of errors
 * This data is to be used in categorising an exception occurring during execution
 */
public class ExceptionCategory
{
    /**
     * User-friendly string for the error category
     */
    private final ErrorCategory errorCategory;

    /**
     * List of regexes - if an exception includes any such keyword in its class name or message the category is a match
     */
    private final ArrayList<Pattern> keywords;

    /**
     * List of error types (essentially sub-categories of errors) associated with this category
     */
    private final ArrayList<ExceptionType> exceptionTypes;

    /**
     * Constructor to create an error category object containing data to be used in categorising occurring exceptions
     * @param errorCategory is the name of the category meant to be end user understandable
     * @param keywords are a list of regexes used in classifying an exception to this category
     * @param exceptionTypes list of error types (subcategories) used in classifying an exception to this category
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ExceptionCategory(@JsonProperty("CategoryName") ErrorCategory errorCategory, @JsonProperty("Keywords") ArrayList<String> keywords, @JsonProperty("Types") ArrayList<ExceptionType> exceptionTypes)
    {
        this.errorCategory = errorCategory;
        this.keywords = new ArrayList<>();
        for (String keyword : keywords)
        {
            this.keywords.add(Pattern.compile(keyword, Pattern.CASE_INSENSITIVE));
        }
        this.exceptionTypes = exceptionTypes;
    }

    /**
     * Method to check if an error category and an occurred exception are a match under a specified matching method
     * @param exception is the error that occurred during execution
     * @param method is the type of exception matching we would like to execute
     * @return true if the exception and category are a match false otherwise.
     */
    public boolean matches(Throwable exception, MatchingMethod method)
    {
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        String name = exception.getClass().getSimpleName();
        switch (method)
        {
            case EXCEPTION_OUTLINE_MATCHING:
                return hasMatchingExceptionOutline(name, message);
            case KEYWORDS_MATCHING:
                return hasMatchingKeywords(name, message);
            case TYPE_NAME_MATCHING:
                return hasMatchingTypeName(name);
            default:
                throw new EngineException("Invalid matching method specified for error handling", ErrorCategory.OTHER_ERROR);
        }
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
        return this.exceptionTypes.stream().anyMatch(type -> type.hasMatchingExceptionOutline(name, message));
    }

    /**
     * check if the exception's class name matches any of this category's Types' exception name regex
     * @param name is the exception's name
     * @return true if the category is a match, false otherwise
     */
    private boolean hasMatchingTypeName(String name)
    {
        return this.exceptionTypes.stream().anyMatch(type -> type.hasMatchingTypeName(name));
    }

    /**
     * @return user-friendly string corresponding to this error category
     */
    public ErrorCategory getErrorCategory()
    {
        return errorCategory;
    }

    /**
     * Class representing an error type which is essentially a sub-category of errors
     * To track the Type for each error change the streams().anyMatch() to enhanced for loops in ErrorCategory
     * and return Type object's name when a successful match is found
     */
    private static class ExceptionType
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
        private final ArrayList<ExceptionOutline> exceptionOutlines;

        /**
         * Constructor to create an error type holding data to be used in categorizing an exception to its correct category
         * @param typeName is the name of this error Type
         * @param typeExceptionRegex is the regex matching exception names to this Type
         * @param exceptionOutlines is the list of exception name and message regex pairs to be used in matching
         */
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public ExceptionType(@JsonProperty("TypeName") String typeName, @JsonProperty("TypeExceptionRegex") String typeExceptionRegex, @JsonProperty("Exceptions") ArrayList<ExceptionOutline> exceptionOutlines)
        {
            this.typeName = typeName;
            this.typeExceptionRegex = Pattern.compile(typeExceptionRegex, Pattern.CASE_INSENSITIVE);
            this.exceptionOutlines = exceptionOutlines;
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
        private static class ExceptionOutline
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
             * Constructor to create an exception outline object
             * @param exceptionName is the simple name of the exception
             * @param exceptionMessage is the regex corresponding to the message in the exception
             */
            @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
            public ExceptionOutline(@JsonProperty("ExceptionName") String exceptionName, @JsonProperty("MessageRegex") String exceptionMessage)
            {
                this.exceptionName = exceptionName;
                this.exceptionMessage = Pattern.compile(exceptionMessage, Pattern.CASE_INSENSITIVE);
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
