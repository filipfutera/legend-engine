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

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Class representing an error type which is essentially a sub-category of errors
 */
public class ErrorType
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
     * Method to check if an occurring exception class name and message match any exception outline associated with this type.
     * @param name is the exception class name
     * @param message is the exception message
     * @return true if the exception matches an outline class name and message regex pair and false otherwise.
     */
    public boolean isExceptionOutlineMatch(String name, String message)
    {
        for (ErrorExceptionOutline exceptionOutline : this.exceptionOutlines)
        {
            if (exceptionOutline.isMatch(name, message))
            {
                return true;
            }
        }
        return false;
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
     * Method to get the regex for matching occuring exception's class names
     * @return the type's class name regex
     */
    public Pattern getTypeExceptionRegex()
    {
        return typeExceptionRegex;
    }

}