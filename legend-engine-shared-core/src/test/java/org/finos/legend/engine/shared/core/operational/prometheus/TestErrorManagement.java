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

import com.fasterxml.jackson.core.JsonGenerationException;
import io.prometheus.client.CollectorRegistry;
import org.apache.http.ConnectionClosedException;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ErrorCategory;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.TestCouldNotBeSkippedException;
import java.util.MissingFormatWidthException;
import java.util.UnknownFormatFlagsException;
import static org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler.toCamelCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TestErrorManagement
{
    private final String[] ERROR_LABEL_NAMES = {"errorLabel", "category", "source", "serviceName"};
    private final String METRIC_NAME = "legend_engine_error_total";
    private final CollectorRegistry METRIC_REGISTRY = MetricsHandler.getMetricsRegistry();
    private final double DELTA = 0d;
    private final String TEST_SERVICE_PATH = "service/remote/getRegistry";

    @After()
    public void clearCounterData()
    {
        MetricsHandler.ERROR_COUNTER.clear();
    }

    @Test
    public void testServiceErrorOriginLabel()
    {
        MetricsHandler.observeError(LoggingEventType.SERVICE_TEST_EXECUTE_ERROR, new Exception(), TEST_SERVICE_PATH);
        String[] labels = {"ServiceTestExecuteError", "UnknownError", "ServiceTestExecute", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorOriginLabel()
    {
        MetricsHandler.observeError(LoggingEventType.SERVICE_TEST_EXECUTE_ERROR, new Exception(), null);
        String[] labels = {"ServiceTestExecuteError", "UnknownError", "ServiceTestExecute", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorWithoutOrigin()
    {
        Exception exception = assertThrows(EngineException.class, () ->
        {
            MetricsHandler.observeError(null, new Exception(), TEST_SERVICE_PATH);
        });
        assertEquals(exception.getMessage(), "Error origin must not be null!");
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithNonGenericException()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new ArithmeticException(), null);
        String[] labels = {"ArithmeticError", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorLabelExtractionWithNonGenericException()
    {
        MetricsHandler.observeError(LoggingEventType.LAMBDA_RETURN_TYPE_ERROR, new NullPointerException(), TEST_SERVICE_PATH);
        labels = new String[]{"NullPointerError", "UnknownError", "LambdaReturnType", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithEngineExceptionWithType()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new EngineException(null, null, EngineErrorType.COMPILATION), null);
        String[] labels = {"CompilationEngineError", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorLabelExtractionWithEngineExceptionWithType()
    {
        MetricsHandler.observeError(LoggingEventType.DSB_EXECUTE_ERROR, new EngineException("unknown message",null, EngineErrorType.PARSER), TEST_SERVICE_PATH);
        String[] labels = new String[]{"ParserEngineError", "UnknownError", "DsbExecute", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithEngineExceptionWithoutTypeWithOrigin()
    {
        MetricsHandler.observeError(LoggingEventType.COMPILE_MODEL_ERROR, new EngineException(null), null);
        String[] labels = {"CompileModelEngineError", "UnknownError", "CompileModel", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithEngineExceptionWithoutTypeWithoutOrigin()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new EngineException(null), null);
        String[] labels = {"CatchAllEngineError", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorLabelExtractionWithRuntimeExceptionWithCause()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new RuntimeException(new ArithmeticException()), TEST_SERVICE_PATH);
        String[] labels = {"ArithmeticError", "UnknownError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithRuntimeExceptionWithoutCause()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new RuntimeException(), null);
        String[] labels = {"CatchAllRuntimeError", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithNestedRuntimeException()
    {
        RuntimeException nestedOtherErrorException = new RuntimeException(new RuntimeException());
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception(nestedOtherErrorException), null);
        String[] labels = {"CatchAllRuntimeError", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithNestedEngineException()
    {
        RuntimeException nestedOtherErrorException = new RuntimeException(new EngineException(""));
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new Exception(nestedOtherErrorException), null);
        String[] labels = {"UnrecognisedEngineError", "UnknownError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorLabelExtractionWithCrossCausingExceptions()
    {
        Exception exceptionOne = new Exception();
        RuntimeException exceptionTwo = new RuntimeException(exceptionOne);
        exceptionOne.initCause(exceptionTwo);
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, exceptionOne, null);
        String[] labels = {"CatchAllRuntimeError", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
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
        String[] labels = {"CatchAllRuntimeError", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationWithCrossCausingExceptions()
    {
        Exception exceptionOne = new Exception();
        Exception exceptionTwo = new Exception(exceptionOne);
        exceptionOne.initCause(exceptionTwo);
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, exceptionOne, null);
        String[] labels = {"CatchAllError", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
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
        String[] labels = {"CatchAllError", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationWithNestedUniqueException()
    {
        RuntimeException nestedOtherErrorException = new RuntimeException(new java.net.SocketTimeoutException("socket timeout"));
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception(nestedOtherErrorException), null);
        String[] labels = {"SocketTimeoutError", "OtherError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationToUserAuthenticationErrorWithExceptionOutlineMatching()
    {
        UnsupportedOperationException permissionsError = new UnsupportedOperationException("credentials issues");
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, permissionsError, null);
        String[] labels = {"UnsupportedOperationError", "UserAuthenticationError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationToUserAuthenticationErrorWithTypeNameMatching()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new IllegalAccessException("user invalid details"), null);
        String[] labels = {"IllegalAccessError", "UserAuthenticationError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationToUserAuthenticationErrorWithKeywordsMatching()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new Exception("some text including kerberos keyword"), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedError", "UserAuthenticationError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationToUserExecutionErrorWithExceptionOutlineMatching()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new Exception("some text including kerberos keyword"), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedError", "UserAuthenticationError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Ignore("no type name patterns recorded for UserExecution error category yet")
    @Test
    public void testErrorCategorizationToUserExecutionErrorWithTypeNameMatching()
    {

    }

    @Test
    public void testServiceErrorCategorizationToUserExecutionErrorWithKeywordsMatching()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new Exception("database schema 'someSchema' not found"), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedError", "UserExecutionError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationToInternalServerErrorWithExceptionOutlineMatching()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new RuntimeException("something has not been configured properly"), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedRuntimeError", "InternalServerError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationToInternalServerErrorWithTypeNameMatching()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new MissingFormatWidthException(""), TEST_SERVICE_PATH);
        String[] labels = {"MissingFormatWidthError", "InternalServerError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationToInternalServerErrorWithKeywordsMatching()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new Exception("overloaded servers"), null);
        String[] labels = {"UnrecognisedError", "InternalServerError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationToServerExecutionErrorWithExceptionOutlineMatching()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new IllegalArgumentException("there was an invalid hexadecimal representation of an ObjectId '123456789'"), null);
        String[] labels = {"IllegalArgumentError", "ServerExecutionError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);

        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new EngineException("Error in 'some::graph': Can't find the profile 'some::profile'"), null);
        labels = new String[]{"UnrecognisedEngineError", "ServerExecutionError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationToServerExecutionErrorWithTypeNameMatching()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new JsonGenerationException("some message"), null);
        String[] labels = {"JsonGenerationError", "ServerExecutionError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationToServerExecutionErrorWithKeywordsMatching()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new Exception("Error in 'some::graph': Couldn't resolve test"), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedError", "ServerExecutionError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationToOtherErrorWithExceptionOutlineMatching()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new IllegalArgumentException("Tests Failed! Error running tests for service 'some/service'"), TEST_SERVICE_PATH);
        String[] labels = {"IllegalArgumentError", "OtherError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationToOtherErrorWithTypeNameMatching()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new ConnectionClosedException(), null);
        String[] labels = {"ConnectionClosedError", "OtherError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationToOtherErrorWithKeywordsMatchingInExceptionName()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new TestCouldNotBeSkippedException(null), null);
        String[] labels = {"TestCouldNotBeSkippedError", "OtherError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorCategorizationToOtherErrorWithKeywordsMatchingInExceptionMessage()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new Exception("some tests have failed!"), null);
        String[] labels = {"UnrecognisedError", "OtherError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationToUnknownErrorByAnyMatching()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new UnknownFormatFlagsException("some unknown error"), TEST_SERVICE_PATH);
        String[] labels = {"UnknownFormatFlagsError", "UnknownError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationMatchingMethodPrioritizationOfExceptionOutlineToKeywords()
    {
        MetricsHandler.observeError(LoggingEventType.DSB_EXECUTE_ERROR, new EngineException("Can't resolve the builder for function 'get/Login/Kerberos"), TEST_SERVICE_PATH);
        String[] labels = {"DsbExecuteEngineError", "ServerExecutionError", "DsbExecute", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationMatchingMethodPrioritizationOfKeywordsToTypeName()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new JsonGenerationException("can't get kerberos authentication"), TEST_SERVICE_PATH);
        String[] labels = {"JsonGenerationError", "UserAuthenticationError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testEnumToUserFriendlyStringConversion()
    {
        assertEquals(toCamelCase(LoggingEventType.PURE_QUERY_EXECUTION_ERROR), "PureQueryExecutionError");
        assertEquals(toCamelCase(LoggingEventType.GENERATE_PLAN_ERROR), "GeneratePlanError");
        assertEquals(toCamelCase(LoggingEventType.UNRECOGNISED_ERROR), "UnrecognisedError");
        assertEquals(toCamelCase(ErrorCategory.USER_AUTHENTICATION_ERROR), "UserAuthenticationError");
        assertEquals(toCamelCase(ErrorCategory.UNKNOWN_ERROR), "UnknownError");
    }

    @Test
    public void testInteractiveCorrectCounterSampleIncrementation()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new Exception(), null);
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new Exception(), null);
        String[] labels = {"UnrecognisedError", "UnknownError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 2, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationExtractingValidCategoryFromEngineException()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new EngineException("some message", ErrorCategory.INTERNAL_SERVER_ERROR), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedEngineError", "InternalServerError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationExtractingUnknownCategoryFromEngineException()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new EngineException("Error in 'some::graph': Couldn't resolve test", ErrorCategory.UNKNOWN_ERROR), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedEngineError", "ServerExecutionError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationExtractingNullCategoryFromEngineException()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new EngineException("Error in 'some::graph': Couldn't resolve test", (ErrorCategory) null), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedEngineError", "ServerExecutionError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationExtractingValidCategoryFromNestedEngineException()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new Exception(new EngineException("some message", ErrorCategory.INTERNAL_SERVER_ERROR)), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedEngineError", "InternalServerError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationMatchingTechniquePrioritizationOfCategorisedExceptionToMatching()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new Exception("kerberos", new EngineException("some message", ErrorCategory.INTERNAL_SERVER_ERROR)), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedEngineError", "InternalServerError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceErrorCategorizationRegexCaseInsensitivity()
    {
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new Exception("KERBEROS"), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedError", "UserAuthenticationError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }
}
