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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
