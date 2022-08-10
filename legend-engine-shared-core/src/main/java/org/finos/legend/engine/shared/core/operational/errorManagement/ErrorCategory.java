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

import java.util.Arrays;

/**
 * User friendly error categories
 */
public enum ErrorCategory
{
    USER_AUTHENTICATION_ERROR,
    USER_EXECUTION_ERROR,
    INTERNAL_SERVER_ERROR,
    SERVER_EXECUTION_ERROR,
    OTHER_ERROR,
    UNKNOWN_ERROR,
    ;

    public String toCamelCase()
    {
        String errorOrigin = this.toString().toLowerCase();
        String[] elements = errorOrigin.split("_");
        StringBuilder output = new StringBuilder();
        for (String element : elements)
        {
            output.append(element.substring(0, 1).toUpperCase()).append(element.substring(1));
        }
        return output.toString();
    }

    public static ErrorCategory valueOfCamelCase(String errorCategory)
    {
        String snakeCaseErrorCategory = String.join("_", Arrays.asList(errorCategory.split("(?=[A-Z])")));
        return ErrorCategory.valueOf(snakeCaseErrorCategory);
    }
}


