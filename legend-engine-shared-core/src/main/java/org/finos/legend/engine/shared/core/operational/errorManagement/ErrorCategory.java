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

import java.util.Map.Entry;
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
    private ArrayList<Pattern> keywords;

    /**
     * List of error types (essentially sub-categories of errors) associated with this category
     */
    private ArrayList<ErrorType> errorTypes;

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
     * Method to check if an error category and an occurred exception are a match
     * @param exception is the error that occurred during execution
     * @return true if the exception and category are a match false otherwise.
     */
    public boolean match(Exception exception)
    {
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        String name = exception.getClass().getSimpleName();
        return matchKeywords(name, message) || matchTypeNames(name) || matchExceptionOutlines(name, message);
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
        for (ErrorType type : this.errorTypes)
        {
            if (type.isExceptionOutlineMatch(name, message)) {
                return true;
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
        for (ErrorType errorType : this.errorTypes)
        {
            Matcher matcher = errorType.getTypeExceptionRegex().matcher(name);
            if (!errorType.getTypeExceptionRegex().toString().equals("") && matcher.find())
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

}




