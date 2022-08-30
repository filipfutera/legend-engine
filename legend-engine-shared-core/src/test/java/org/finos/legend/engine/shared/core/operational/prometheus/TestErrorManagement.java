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

package org.finos.legend.engine.shared.core.operational.prometheus;

import io.prometheus.client.CollectorRegistry;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.junit.After;
import org.junit.Test;
import org.junit.TestCouldNotBeSkippedException;
import java.util.UnknownFormatFlagsException;
import static org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler.toCamelCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TestErrorManagement
{
    private final String[] COUNTER_LABEL_NAMES = {"exceptionClass", "category", "source", "serviceName"};
    private final String METRIC_NAME = "legend_engine_error_total";
    private final CollectorRegistry METRIC_REGISTRY = MetricsHandler.getMetricsRegistry();
    private final double DELTA = 0d;
    private final String TEST_SERVICE_PATH = "service/remote/getRegistry";

    @After()
    public void clearCounterData()
    {
        MetricsHandler.EXCEPTION_ERROR_COUNTER.clear();
    }

    @Test
    public void testServiceErrorOriginLabel()
    {
        MetricsHandler.observeError(LoggingEventType.SERVICE_TEST_EXECUTE_ERROR, new Exception(), TEST_SERVICE_PATH);
        String[] labels = {"ServiceTestExecuteException", "UnknownError", "ServiceTestExecute", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorOriginLabel()
    {
        MetricsHandler.observeError(LoggingEventType.SERVICE_TEST_EXECUTE_ERROR, new Exception(), null);
        String[] labels = {"ServiceTestExecuteException", "UnknownError", "ServiceTestExecute", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorWithoutOrigin()
    {
        Exception exception = assertThrows(EngineException.class, () ->
                MetricsHandler.observeError(null, new Exception(), TEST_SERVICE_PATH));
        assertEquals(exception.getMessage(), "Exception origin must not be null!");
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithNonGenericException()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new ArithmeticException(), null);
        String[] labels = {"ArithmeticException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorLabelExtractionWithNonGenericException()
    {
        MetricsHandler.observeError(LoggingEventType.LAMBDA_RETURN_TYPE_ERROR, new NullPointerException(), TEST_SERVICE_PATH);
        String[] labels = new String[]{"NullPointerException", "UnknownError", "LambdaReturnType", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithEngineExceptionWithType()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new EngineException(null, null, EngineErrorType.COMPILATION), null);
        String[] labels = {"CompilationEngineException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorLabelExtractionWithEngineExceptionWithType()
    {
        MetricsHandler.observeError(LoggingEventType.DSB_EXECUTE_ERROR, new EngineException("unknown message",null, EngineErrorType.PARSER), TEST_SERVICE_PATH);
        String[] labels = new String[]{"ParserEngineException", "UnknownError", "DsbExecute", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithEngineExceptionWithoutTypeWithOrigin()
    {
        MetricsHandler.observeError(LoggingEventType.COMPILE_MODEL_ERROR, new EngineException(null), null);
        String[] labels = {"CompileModelEngineException", "UnknownError", "CompileModel", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithEngineExceptionWithoutTypeWithoutOrigin()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new EngineException(null), null);
        String[] labels = {"CatchAllEngineException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorLabelExtractionWithRuntimeExceptionWithCause()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new RuntimeException(new ArithmeticException()), TEST_SERVICE_PATH);
        String[] labels = {"ArithmeticException", "UnknownError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithRuntimeExceptionWithoutCause()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new RuntimeException(), null);
        String[] labels = {"CatchAllRuntimeException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithNestedRuntimeException()
    {
        RuntimeException nestedOtherErrorException = new RuntimeException(new RuntimeException());
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception(nestedOtherErrorException), null);
        String[] labels = {"CatchAllRuntimeException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithNestedEngineException()
    {
        RuntimeException nestedOtherErrorException = new RuntimeException(new EngineException(""));
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception(nestedOtherErrorException), null);
        String[] labels = {"CatchAllEngineException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithCrossCausingExceptions()
    {
        Exception exceptionOne = new Exception();
        RuntimeException exceptionTwo = new RuntimeException(exceptionOne);
        exceptionOne.initCause(exceptionTwo);
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, exceptionOne, null);
        String[] labels = {"CatchAllRuntimeException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithLoopingExceptionCause()
    {
        Exception exceptionOne = new Exception();
        Exception exceptionTwo = new Exception();
        RuntimeException exceptionThree = new RuntimeException(exceptionOne);
        exceptionOne.initCause(exceptionTwo);
        exceptionTwo.initCause(exceptionThree);
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, exceptionOne, null);
        String[] labels = {"CatchAllRuntimeException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationWithCrossCausingExceptions()
    {
        Exception exceptionOne = new Exception();
        Exception exceptionTwo = new Exception(exceptionOne);
        exceptionOne.initCause(exceptionTwo);
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, exceptionOne, null);
        String[] labels = {"CatchAllException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationWithLoopingExceptionCause()
    {
        Exception exceptionOne = new Exception();
        Exception exceptionTwo = new Exception();
        Exception exceptionThree = new Exception(exceptionOne);
        exceptionOne.initCause(exceptionTwo);
        exceptionTwo.initCause(exceptionThree);
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, exceptionOne, null);
        String[] labels = {"CatchAllException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationWithNestedUniqueException()
    {
        RuntimeException nestedOtherErrorException = new RuntimeException(new java.net.SocketTimeoutException("socket timeout"));
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception(nestedOtherErrorException), null);
        String[] labels = {"SocketTimeoutException", "OtherError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationToUserAuthenticationErrorWithExceptionOutlineMatching()
    {
        UnsupportedOperationException permissionsError = new UnsupportedOperationException("credentials issues");
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, permissionsError, null);
        String[] labels = {"UnsupportedOperationException", "UserAuthenticationError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationToUserAuthenticationErrorWithKeywordsMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("some text including kerberos keyword"), TEST_SERVICE_PATH);
        String[] labels = {"CatchAllException", "UserAuthenticationError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationToUserExecutionErrorWithExceptionOutlineMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("some text including kerberos keyword"), TEST_SERVICE_PATH);
        String[] labels = {"CatchAllException", "UserAuthenticationError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationToUserExecutionErrorWithKeywordsMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("database schema 'someSchema' not found"), TEST_SERVICE_PATH);
        String[] labels = {"CatchAllException", "UserExecutionError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationToInternalServerErrorWithExceptionOutlineMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new RuntimeException("something has not been configured properly"), TEST_SERVICE_PATH);
        String[] labels = {"CatchAllRuntimeException", "InternalServerError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationToInternalServerErrorWithKeywordsMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("overloaded servers"), null);
        String[] labels = {"CatchAllException", "InternalServerError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationToServerExecutionErrorWithExceptionOutlineMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new RuntimeException("Input stream was not provided"), null);
        String[] labels = {"CatchAllRuntimeException", "ServerExecutionError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);

        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new EngineException("Error in 'some::graph': Can't find the profile 'some::profile'"), null);
        labels = new String[]{"CatchAllEngineException", "ServerExecutionError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationToServerExecutionErrorWithKeywordsMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("Error in 'some::graph': Couldn't resolve test"), TEST_SERVICE_PATH);
        String[] labels = {"CatchAllException", "ServerExecutionError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationToOtherErrorWithExceptionOutlineMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new IllegalArgumentException("Tests Failed! Error running tests for service 'some/service'"), TEST_SERVICE_PATH);
        String[] labels = {"IllegalArgumentException", "OtherError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationToOtherErrorWithKeywordsMatchingInTestScenario()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new TestCouldNotBeSkippedException(null), null);
        String[] labels = {"TestCouldNotBeSkippedException", "OtherError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationToOtherErrorWithKeywordsMatchingInExceptionMessage()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("some tests have failed!"), null);
        String[] labels = {"CatchAllException", "OtherError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationToUnknownErrorByAnyMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new UnknownFormatFlagsException("some unknown error"), TEST_SERVICE_PATH);
        String[] labels = {"UnknownFormatFlagsException", "UnknownError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationMatchingMethodPrioritizationOfExceptionOutlineToKeywords()
    {
        MetricsHandler.observeError(LoggingEventType.DSB_EXECUTE_ERROR, new EngineException("Can't resolve the builder for function 'get/Login/Kerberos"), TEST_SERVICE_PATH);
        String[] labels = {"DsbExecuteEngineException", "ServerExecutionError", "DsbExecute", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testEnumToUserFriendlyStringConversion()
    {
        assertEquals(toCamelCase(LoggingEventType.PURE_QUERY_EXECUTE_ERROR), "PureQueryExecuteError");
        assertEquals(toCamelCase(LoggingEventType.GENERATE_PLAN_ERROR), "GeneratePlanError");
        assertEquals(toCamelCase(LoggingEventType.CATCH_ALL), "CatchAll");
        assertEquals(toCamelCase(ExceptionCategory.USER_AUTHENTICATION_ERROR), "UserAuthenticationError");
        assertEquals(toCamelCase(ExceptionCategory.UNKNOWN_ERROR), "UnknownError");
    }

    @Test
    public void testInteractiveCorrectCounterSampleIncrementation()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception(), null);
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception(), null);
        String[] labels = {"CatchAllException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 2, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationExtractingValidCategoryFromEngineException()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new EngineException("some message", ExceptionCategory.INTERNAL_SERVER_ERROR), TEST_SERVICE_PATH);
        String[] labels = {"CatchAllEngineException", "InternalServerError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationExtractingUnknownCategoryFromEngineException()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new EngineException("Error in 'some::graph': Couldn't resolve test", ExceptionCategory.UNKNOWN_ERROR), TEST_SERVICE_PATH);
        String[] labels = {"CatchAllEngineException", "ServerExecutionError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationExtractingNullCategoryFromEngineException()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new EngineException("Error in 'some::graph': Couldn't resolve test", (ExceptionCategory) null), TEST_SERVICE_PATH);
        String[] labels = {"CatchAllEngineException", "ServerExecutionError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationExtractingValidCategoryFromNestedEngineException()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception(new EngineException("some message", ExceptionCategory.INTERNAL_SERVER_ERROR)), TEST_SERVICE_PATH);
        String[] labels = {"CatchAllEngineException", "InternalServerError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationMatchingTechniquePrioritizationOfCategorisedExceptionToMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("kerberos", new EngineException("some message", ExceptionCategory.INTERNAL_SERVER_ERROR)), TEST_SERVICE_PATH);
        String[] labels = {"CatchAllEngineException", "InternalServerError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationRegexCaseInsensitivity()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("KERBEROS"), TEST_SERVICE_PATH);
        String[] labels = {"CatchAllException", "UserAuthenticationError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    private enum ErrorOrigin
    {
        ERROR_MANAGEMENT_TEST_ERROR
    }

    @Test
    public void testErrorOriginExtensibilityBeyondLoggingEventType()
    {
        MetricsHandler.observeError(ErrorOrigin.ERROR_MANAGEMENT_TEST_ERROR, new Exception("KERBEROS"), TEST_SERVICE_PATH);
        String[] labels = {"ErrorManagementTestException", "UserAuthenticationError", "ErrorManagementTest", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testOnlyFullMatchingForExceptionName()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("Can't find database"), TEST_SERVICE_PATH);
        String[] labels = {"CatchAllException", "UnknownError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

}
