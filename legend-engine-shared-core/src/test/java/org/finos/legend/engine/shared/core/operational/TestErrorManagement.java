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

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ErrorOrigin;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.junit.After;
import org.junit.Test;

import java.util.Enumeration;

import static org.junit.Assert.assertEquals;

public class TestErrorManagement
{
    private final String[] ERROR_LABEL_NAMES = {"errorLabel", "category", "source", "serviceName"};
    private final String METRIC_NAME = "legend_engine_error_total";
    private final CollectorRegistry METRIC_REGISTRY = MetricsHandler.getMetricsRegistry();
    private final double DELTA = 0.000001d;
    private final String TEST_SERVICE_PATH_ONE = "service/remote/getRegistry";
    private final String TEST_SERVICE_PATH_TWO = "service/remote/getComputers";

    @After
    public void clearCounterData()
    {
        //MetricsHandler.getErrorCounter().clear();
        Enumeration<Collector.MetricFamilySamples> iterator = METRIC_REGISTRY.metricFamilySamples();
        while (iterator.hasMoreElements()) {
            Collector.MetricFamilySamples metricFamilySamples = iterator.nextElement();
            System.out.println(metricFamilySamples.name.equals(METRIC_NAME));
            if (metricFamilySamples.name.equals(METRIC_NAME)) {
                metricFamilySamples.samples.clear();
            }
        }
    }


    @Test
    public void testErrorWithValidOriginValidServicePattern()
    {
        MetricsHandler.observeError(ErrorOrigin.SERVICE_TEST_EXECUTE, new Exception(), TEST_SERVICE_PATH_ONE);
        String[] labels = {"Error", "UnknownError", "Service", TEST_SERVICE_PATH_ONE};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorWithValidOriginInvalidServicePattern()
    {
        MetricsHandler.observeError(ErrorOrigin.SERVICE_TEST_EXECUTE, new Exception(), null);
        String[] labels = {"Error", "UnknownError", ErrorOrigin.SERVICE_TEST_EXECUTE.toFriendlyString(), "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorWithInvalidOriginValidServicePattern()
    {
        MetricsHandler.observeError(null, new Exception(), TEST_SERVICE_PATH_TWO);
        String[] labels = {"Error", "UnknownError", "Service", TEST_SERVICE_PATH_TWO};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorWithInvalidOriginInvalidServicePattern()
    {
        MetricsHandler.observeError(null, new Exception(), null);
        String[] labels = {"Error", "UnknownError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelWithUniqueException()
    {
        MetricsHandler.observeError(null, new ArithmeticException(), null);
        String[] labels = {"ArithmeticError", "UnknownError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);

        MetricsHandler.observeError(ErrorOrigin.LAMBDA_RETURN_TYPE, new NullPointerException(), TEST_SERVICE_PATH_ONE);
        labels = new String[]{"NullPointerError", "UnknownError", "Service", TEST_SERVICE_PATH_ONE};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelWithEngineExceptionWithType()
    {
        MetricsHandler.observeError(null, new EngineException(null,null, EngineErrorType.COMPILATION), null);
        String[] labels = {"CompilationEngineError", "UnknownError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);

        MetricsHandler.observeError(ErrorOrigin.DSB_EXECUTE, new EngineException("test message",null, EngineErrorType.PARSER), TEST_SERVICE_PATH_ONE);
        labels = new String[]{"ParserEngineError", "UnknownError", "Service", TEST_SERVICE_PATH_ONE};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelWithRuntimeExceptionWithCause()
    {
        MetricsHandler.observeError(null, new RuntimeException(new ArithmeticException()), TEST_SERVICE_PATH_ONE);
        String[] labels = {"ArithmeticError", "UnknownError", "Service", TEST_SERVICE_PATH_ONE};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelWithRuntimeExceptionWithoutCause()
    {
        MetricsHandler.observeError(null, new RuntimeException(), null);
        String[] labels = {"UnknownRuntimeError", "UnknownError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    //test for each category - matching by keywords, typenameregex and an exceptionoutline -
    //then test looping exceptions, and convoluted exceptions.

}
