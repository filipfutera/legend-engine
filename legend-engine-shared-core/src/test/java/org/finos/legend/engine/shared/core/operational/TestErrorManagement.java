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
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ErrorOrigin;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestErrorManagement
{
    private final String[] ERROR_LABEL_NAMES = {"errorLabel", "category", "source", "serviceName"};
    private final String METRIC_NAME = "legend_engine_error_total";
    private final CollectorRegistry METRIC_REGISTRY = MetricsHandler.getMetricsRegistry();
    private final double DELTA = 0.000001d;
    private final String TEST_SERVICE_PATH = "service/remote/getRegistry";

    @After
    public void clearCounterData()
    {
        MetricsHandler.getErrorCounter().clear();
    }


    @Test
    public void testErrorWithValidOriginValidServicePattern()
    {
        MetricsHandler.observeError(ErrorOrigin.SERVICE_TEST_EXECUTE, new Exception(), TEST_SERVICE_PATH);
        String[] labels = {"Error", "UnknownError", "Service", TEST_SERVICE_PATH};
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
        MetricsHandler.observeError(null, new Exception(), TEST_SERVICE_PATH);
        String[] labels = {"Error", "UnknownError", "Service", TEST_SERVICE_PATH};
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

        MetricsHandler.observeError(ErrorOrigin.LAMBDA_RETURN_TYPE, new NullPointerException(), TEST_SERVICE_PATH);
        labels = new String[]{"NullPointerError", "UnknownError", "Service", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelWithEngineExceptionWithType()
    {
        MetricsHandler.observeError(null, new EngineException(null, null, EngineErrorType.COMPILATION), null);
        String[] labels = {"CompilationEngineError", "UnknownError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);

        MetricsHandler.observeError(ErrorOrigin.DSB_EXECUTE, new EngineException("unknown message",null, EngineErrorType.PARSER), TEST_SERVICE_PATH);
        labels = new String[]{"ParserEngineError", "UnknownError", "Service", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelWithRuntimeExceptionWithCause()
    {
        MetricsHandler.observeError(null, new RuntimeException(new ArithmeticException()), TEST_SERVICE_PATH);
        String[] labels = {"ArithmeticError", "UnknownError", "Service", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelWithRuntimeExceptionWithoutCause()
    {
        MetricsHandler.observeError(null, new RuntimeException(), null);
        String[] labels = {"UnknownRuntimeError", "UnknownError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testCategoryLabelWithCrossCausingExceptionCause()
    {
        Exception exceptionOne = new Exception();
        Exception exceptionTwo = new Exception(exceptionOne);
        exceptionOne.initCause(exceptionTwo);
        MetricsHandler.observeError(null, exceptionOne, null);
        String[] labels = {"Error", "UnknownError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testCategoryLabelWithLoopingExceptionCause()
    {
        Exception exceptionThree = new Exception();
        Exception exceptionFour = new Exception();
        Exception exceptionFive = new Exception(exceptionThree);
        exceptionThree.initCause(exceptionFour);
        exceptionFour.initCause(exceptionFive);
        exceptionThree.initCause(exceptionThree);
        MetricsHandler.observeError(null, exceptionThree, null);
        String[] labels = {"RuntimeError", "UnknownError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testCategoryLabelWithConvolutedException()
    {
         
    }

    @Test
    public void testUserAuthenticationErrorExceptionOutlineMatching()
    {

    }

    @Test
    public void testUserAuthenticationErrorTypeNameMatching()
    {

    }

    @Test
    public void testUserAuthenticationErrorKeywordsMatching()
    {

    }

    @Test
    public void testUserExecutionErrorExceptionOutlineMatching()
    {

    }

    @Test
    public void testUserExecutionErrorTypeNameMatching()
    {

    }

    @Test
    public void testUserExecutionErrorKeywordsMatching()
    {

    }

    @Test
    public void testInternalServerErrorExceptionOutlineMatching()
    {

    }

    @Test
    public void testInternalServerErrorTypeNameMatching()
    {

    }

    @Test
    public void testInternalServerErrorKeywordsMatching()
    {

    }

    @Test
    public void testServerExecutionErrorExceptionOutlineMatching()
    {

    }

    @Test
    public void testServerExecutionErrorTypeNameMatching()
    {

    }

    @Test
    public void testServerExecutionErrorKeywordsMatching()
    {

    }

    @Test
    public void testOtherErrorExceptionOutlineMatching()
    {

    }

    @Test
    public void testOtherErrorTypeNameMatching()
    {

    }

    @Test
    public void testOtherErrorKeywordsMatching()
    {

    }

    @Test
    public void testUnknownErrorMatching()
    {

    }


    //test for each category - matching by keywords, typenameregex and an exceptionoutline -
    //then test looping exceptions, and convoluted exceptions.

}
