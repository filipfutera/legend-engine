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

import com.fasterxml.jackson.core.JsonGenerationException;
import io.prometheus.client.CollectorRegistry;
import org.apache.http.ConnectionClosedException;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ErrorOrigin;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.ietf.jgss.GSSException;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.TestCouldNotBeSkippedException;
import org.yaml.snakeyaml.error.MissingEnvironmentVariableException;

import java.io.IOException;
import java.util.UnknownFormatFlagsException;

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
    public void testErrorLabelWithEngineExceptionWithoutTypeWithOrigin()
    {

    }

    @Test
    public void testErrorLabelWithEngineExceptionWithoutTypeWithoutOrigin()
    {

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
        MetricsHandler.observeError(null, exceptionThree, null);
        String[] labels = {"Error", "UnknownError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testCategoryLabelWithNestedException()
    {
        RuntimeException nestedOtherErrorException = new RuntimeException(new java.net.SocketTimeoutException("socket timeout"));
        MetricsHandler.observeError(null, new Exception(nestedOtherErrorException), null);
        String[] labels = {"Error", "OtherError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testUserAuthenticationErrorExceptionOutlineMatching()
    {
        RuntimeException permissionsError = new RuntimeException("some_user is not part of write service for some_service");
        MetricsHandler.observeError(null, permissionsError, null);
        String[] labels = {"UnknownRuntimeError", "UserAuthenticationError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testUserAuthenticationErrorTypeNameMatching()
    {
        MetricsHandler.observeError(null, new GSSException(1), null);
        String[] labels = {"GSSError", "UserAuthenticationError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testUserAuthenticationErrorKeywordsMatching()
    {
        MetricsHandler.observeError(null, new Exception("some text including kerberos keyword"), null);
        String[] labels = {"Error", "UserAuthenticationError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testUserExecutionErrorExceptionOutlineMatching()
    {
        MetricsHandler.observeError(null, new Exception("some text including kerberos keyword"), null);
        String[] labels = {"Error", "UserAuthenticationError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Ignore("no type name patterns recorded for UserExecution error category yet")
    @Test
    public void testUserExecutionErrorTypeNameMatching()
    {

    }

    @Test
    public void testUserExecutionErrorKeywordsMatching()
    {
        MetricsHandler.observeError(null, new Exception("database schema 'someSchema' not found"), null);
        String[] labels = {"Error", "UserExecutionError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInternalServerErrorExceptionOutlineMatching()
    {
        MetricsHandler.observeError(null, new IOException("Server returned HTTP response code: 500 for URL 'https://someUrl.com/get'"), null);
        String[] labels = {"IOError", "InternalServerError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInternalServerErrorTypeNameMatching()
    {
        MetricsHandler.observeError(null, new MissingEnvironmentVariableException("Env_Var"), null);
        String[] labels = {"MissingEnvironmentVariableError", "InternalServerError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInternalServerErrorKeywordsMatching()
    {
        MetricsHandler.observeError(null, new Exception("unreachable proxy"), null);
        String[] labels = {"Error", "InternalServerError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServerExecutionErrorExceptionOutlineMatching()
    {
        MetricsHandler.observeError(null, new IllegalArgumentException("there was an invalid hexadecimal representation of an ObjectId '123456789'"), null);
        String[] labels = {"IllegalArgumentError", "ServerExecutionError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);

        MetricsHandler.observeError(null, new EngineException("Error in 'some::graph': Can't find the profile 'some::profile'"), null);
        labels = new String[]{"UnknownEngineError", "ServerExecutionError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServerExecutionErrorTypeNameMatching()
    {
        MetricsHandler.observeError(null, new JsonGenerationException("some message"), null);
        String[] labels = {"JsonGenerationError", "ServerExecutionError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServerExecutionErrorKeywordsMatching()
    {
        MetricsHandler.observeError(null, new Exception("Error in 'some::graph': Couldn't resolve test"), null);
        String[] labels = {"Error", "ServerExecutionError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testOtherErrorExceptionOutlineMatching()
    {
        MetricsHandler.observeError(null, new IllegalArgumentException("Tests Failed! Error running tests for service 'some/service'"), null);
        String[] labels = {"IllegalArgumentError", "OtherError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testOtherErrorTypeNameMatching()
    {
        MetricsHandler.observeError(null, new ConnectionClosedException(), null);
        String[] labels = {"ConnectionClosedError", "OtherError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testOtherErrorKeywordsInNameMatching()
    {
        MetricsHandler.observeError(null, new TestCouldNotBeSkippedException(null), null);
        String[] labels = {"TestCouldNotBeSkippedError", "OtherError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testOtherErrorKeywordsInMessageMatching()
    {
        MetricsHandler.observeError(null, new Exception("some tests have failed!"), null);
        String[] labels = {"Error", "OtherError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testUnknownErrorMatching()
    {
        MetricsHandler.observeError(null, new UnknownFormatFlagsException("some unknown error"), null);
        String[] labels = {"UnknownFormatFlagsError", "UnknownError", "Unknown", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testExceptionOutlineToKeywordMatchingPriority()
    {
        MetricsHandler.observeError(ErrorOrigin.DSB_EXECUTE, new EngineException("Can't resolve the builder for function 'get/Login/Username"), TEST_SERVICE_PATH);
        String[] labels = {"DsbExecuteEngineError", "ServerExecutionError", "Serve", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testKeywordToTypeNameMatchingPriority()
    {
        //test some sql error with column 'loginUsername' and check that it is server error not user authentication
    }

    @Test
    public void testJSONParsing()
    {

    }

}
