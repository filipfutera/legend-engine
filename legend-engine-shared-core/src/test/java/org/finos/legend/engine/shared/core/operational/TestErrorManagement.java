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
import org.finos.legend.engine.shared.core.operational.errorManagement.ErrorOrigin;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class TestErrorManagement
{
    private final String[] ERROR_LABEL_NAMES = {"errorLabel", "category", "source", "serviceName"};
    private final String METRIC_NAME = "legend_engine_error_total";
    private final CollectorRegistry METRIC_REGISTRY = MetricsHandler.getMetricsRegistry();
    private final double DELTA = 0.000001d;
    private final String TEST_SERVICE_PATH = "service/remote/getRegistry";
    private final HashMap<String[],Double> metricCounterTracker = new HashMap<>();


    private void incrementMetricCounterTracker(String[] labels)
    {
        metricCounterTracker.put(labels, metricCounterTracker.containsKey(labels) ? metricCounterTracker.get(labels)+1 : 1);
    }

    @Test
    public void testErrorWithValidOriginValidServicePattern()
    {
        MetricsHandler.observeError(ErrorOrigin.SERVICE_TEST_EXECUTE, new Exception(), TEST_SERVICE_PATH);
        String[] labels = {"Error", "UnknownError", "Service", TEST_SERVICE_PATH};
        incrementMetricCounterTracker(labels);
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), metricCounterTracker.get(labels), DELTA);
    }

    @Test
    public void testErrorWithValidOriginInvalidServicePattern()
    {
        MetricsHandler.observeError(ErrorOrigin.SERVICE_TEST_EXECUTE, new Exception(), null);
        String[] labels = {"Error", "UnknownError", ErrorOrigin.SERVICE_TEST_EXECUTE.toFriendlyString(), "N/A"};
        incrementMetricCounterTracker(labels);
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), metricCounterTracker.get(labels), DELTA);
    }

    @Test
    public void testErrorWithInvalidOriginValidServicePattern()
    {
        MetricsHandler.observeError(null, new Exception(), TEST_SERVICE_PATH);
        String[] labels = {"Error", "UnknownError", "Service", TEST_SERVICE_PATH};
        incrementMetricCounterTracker(labels);
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), metricCounterTracker.get(labels), DELTA);
    }

    @Test
    public void testErrorWithInvalidOriginInvalidServicePattern()
    {
        MetricsHandler.observeError(null, new Exception(), null);
        String[] labels = {"Error", "UnknownError", "Unknown", "N/A"};
        incrementMetricCounterTracker(labels);
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), metricCounterTracker.get(labels), DELTA);
    }

    @Test
    public void testErrorLabelWithUniqueException()
    {
        MetricsHandler.observeError(null, new ArithmeticException(), null);
        String[] labels = {"ArithmeticError", "UnknownError", "Unknown", "N/A"};
        incrementMetricCounterTracker(labels);
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), metricCounterTracker.get(labels), DELTA);

        MetricsHandler.observeError(ErrorOrigin.LAMBDA_RETURN_TYPE, new NullPointerException(), TEST_SERVICE_PATH);
        labels = new String[]{"NullPointerError", "UnknownError", ErrorOrigin.LAMBDA_RETURN_TYPE.toFriendlyString(), TEST_SERVICE_PATH};
        incrementMetricCounterTracker(labels);
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), metricCounterTracker.get(labels), DELTA);

    }

}
