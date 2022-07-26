// Copyright 2020 Goldman Sachs
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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErrorCategory
{
    private String friendlyName;
    private final HashMap<String, List<Pair<String, Pattern>>> exceptionDataMap = new HashMap<>();
    private ArrayList<Pattern> keywords = new ArrayList<>();
    private final HashMap<String, Pattern> typeRegexMap = new HashMap<>();

    public ErrorCategory(JSONObject errorCategory)
    {
        this.friendlyName = errorCategory.get("CategoryName").toString();

        // Get and Cast Category Regex Keywords
        JSONArray regexKeywords = ((JSONArray) errorCategory.get("Keywords"));
        for (Object regexKeyword : regexKeywords)
        {
            this.keywords.add(Pattern.compile(regexKeyword.toString(), Pattern.CASE_INSENSITIVE));
        }

        JSONArray types = (JSONArray) errorCategory.get("Types");
        Iterator iterator = types.iterator();
        while (iterator.hasNext())
        {
            JSONObject type = (JSONObject) iterator.next();
            String typeName = type.get("TypeName").toString();
            Pattern typeExceptionRegex = Pattern.compile(type.get("TypeExceptionRegex").toString(), Pattern.CASE_INSENSITIVE);
            typeRegexMap.put(typeName, typeExceptionRegex);

            JSONArray jsonExceptions = (JSONArray) type.get("Exceptions");
            Iterator it = jsonExceptions.iterator();
            ArrayList<Pair<String,Pattern>> exceptions = new ArrayList();
            while (it.hasNext())
            {
                JSONObject exceptionData = (JSONObject) it.next();
                String exceptionName = exceptionData.get("ExceptionName").toString();
                String exceptionMessageRegex = exceptionData.get("MessageRegex").toString();
                exceptions.add(new Pair<>(exceptionName, Pattern.compile(exceptionMessageRegex, Pattern.CASE_INSENSITIVE)));
            }
            exceptionDataMap.put(typeName, exceptions);
        }
    }

    public boolean match(Exception exception) {
        Exception ex = exception;
        boolean rerun = false;
        while (!rerun)
        {
            rerun = !ex.equals(exception);
            //check if exception matches any keywords in exception message and name
            for (Pattern keyword : keywords)
            {
                if (keyword.matcher(ex.getMessage()).find() || keyword.matcher(ex.getClass().getSimpleName()).find())
                {
                    return true;
                }
            }

            // check if exception matches any exception name and message pair
            for (Entry<String, List<Pair<String, Pattern>>> entry : exceptionDataMap.entrySet())
            {
                String type = entry.getKey();
                for (Pair<String, Pattern> exceptionData : entry.getValue())
                {
                    Matcher matcher = exceptionData.exceptionMessage.matcher(ex.getMessage());
                    if (ex.getClass().getSimpleName().equals(exceptionData.exceptionName) && matcher.find())
                    {
                        return true;
                    }
                }
            }

            // check if exception matches any general exception name regex
            for (Entry<String, Pattern> exceptionRegex : typeRegexMap.entrySet())
            {
                String type = exceptionRegex.getKey();
                Pattern regex = exceptionRegex.getValue();
                Matcher matcher = regex.matcher(ex.getClass().getSimpleName());
                if (!regex.toString().equals("") && matcher.find())
                {
                    return true;
                }
            }

            //update exception to rerun once with exception cause if no match found
            ex = (Exception) ex.getCause();
        }
        return false;
    }

    public String getFriendlyName() {
        return friendlyName;
    }
    
    public class Pair<S, P>
    {

        public final String exceptionName;
        public final Pattern exceptionMessage;

        public Pair(String exceptionName, Pattern exceptionMessage)
        {
            this.exceptionName = exceptionName;
            this.exceptionMessage = exceptionMessage;
        }
    }
}
