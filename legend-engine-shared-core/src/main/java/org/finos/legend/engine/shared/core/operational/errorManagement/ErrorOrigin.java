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

public enum ErrorOrigin
{
    PURE_QUERY_EXECUTION,
    GENERATE_PLAN,
    LAMBDA_RETURN_TYPE,

    COMPILE_MODEL,
    MODEL_RESOLVE,

    SERVICE_TEST_EXECUTE,
    SERVICE_EXECUTE,

    TDS_PROTOCOL,
    TDS_EXECUTE,
    TDS_GENERATE_CODE,
    TDS_SCHEMA,
    TDS_LAMBDA,
    TDS_METADATA,
    TDS_INPUTS,

    DSB_EXECUTE,

    UNRECOGNISED,
    ;

    public String toFriendlyString()
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
}
