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

package org.finos.legend.engine.shared.core.operational;

import io.prometheus.client.CollectorRegistry;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestErrorManagement
{
    private final String[] ERROR_LABEL_NAMES = {"errorLabel", "category", "source", "serviceName"};
    private final String METRIC_NAME = "legend_engine_error_total";
    private final CollectorRegistry METRIC_REGISTRY = MetricsHandler.getMetricsRegistry();
    private final double DELTA = 0.000001d;

    @Test
    public void testErrorWithValidServicePattern()
    {
        String servicePath = "service/remote/getRegistry";
        MetricsHandler.observeError(null, new Exception(), servicePath);
        String[] expectedLabels = {"Error", "UnknownError", "Service", servicePath};
        float count = METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, expectedLabels).floatValue();
        assertEquals("Valid service pattern is not labeled correctly", count, 1.0, DELTA);
    }


}
