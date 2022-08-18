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
    public void testErrorWithoutOrigin() throws Exception
    {
        MetricsHandler.observeError(null, new Exception(), TEST_SERVICE_PATH);
    }

    @Test
    public void testErrorLabelExtractionWithNonGenericException()
    {
        MetricsHandler.observeError(null, new ArithmeticException(), null);
        String[] labels = {"ArithmeticError", "UnknownError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);

        MetricsHandler.observeError(LoggingEventType.LAMBDA_RETURN_TYPE_ERROR, new NullPointerException(), TEST_SERVICE_PATH);
        labels = new String[]{"NullPointerError", "UnknownError", "LambdaReturnType", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelExtractionWithEngineExceptionWithType()
    {
        MetricsHandler.observeError(null, new EngineException(null, null, EngineErrorType.COMPILATION), null);
        String[] labels = {"CompilationEngineError", "UnknownError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);

        MetricsHandler.observeError(LoggingEventType.DSB_EXECUTE_ERROR, new EngineException("unknown message",null, EngineErrorType.PARSER), TEST_SERVICE_PATH);
        labels = new String[]{"ParserEngineError", "UnknownError", "DsbExecute", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelExtractionWithEngineExceptionWithoutTypeWithOrigin()
    {
        MetricsHandler.observeError(LoggingEventType.COMPILE_MODEL_ERROR, new EngineException(null), null);
        String[] labels = {"CompileModelEngineError", "UnknownError", "CompileModel", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelExtractionWithEngineExceptionWithoutTypeWithoutOrigin()
    {
        MetricsHandler.observeError(null, new EngineException(null), null);
        String[] labels = {"UnrecognisedEngineError", "UnknownError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelExtractionWithRuntimeExceptionWithCause()
    {
        MetricsHandler.observeError(null, new RuntimeException(new ArithmeticException()), TEST_SERVICE_PATH);
        String[] labels = {"ArithmeticError", "UnknownError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelExtractionWithRuntimeExceptionWithoutCause()
    {
        MetricsHandler.observeError(null, new RuntimeException(), null);
        String[] labels = {"UnrecognisedRuntimeError", "UnknownError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelExtractionWithNestedRuntimeException()
    {
        RuntimeException nestedOtherErrorException = new RuntimeException(new RuntimeException());
        MetricsHandler.observeError(null, new Exception(nestedOtherErrorException), null);
        String[] labels = {"UnrecognisedRuntimeError", "UnknownError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelExtractionWithNestedEngineException()
    {
        RuntimeException nestedOtherErrorException = new RuntimeException(new EngineException(""));
        MetricsHandler.observeError(LoggingEventType.UNRECOGNISED_ERROR, new Exception(nestedOtherErrorException), null);
        String[] labels = {"UnrecognisedEngineError", "UnknownError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelExtractionWithCrossCausingExceptions()
    {
        Exception exceptionOne = new Exception();
        RuntimeException exceptionTwo = new RuntimeException(exceptionOne);
        exceptionOne.initCause(exceptionTwo);
        MetricsHandler.observeError(null, exceptionOne, null);
        String[] labels = {"UnrecognisedRuntimeError", "UnknownError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorLabelExtractionWithLoopingExceptionCause()
    {
        Exception exceptionOne = new Exception();
        Exception exceptionTwo = new Exception();
        RuntimeException exceptionThree = new RuntimeException(exceptionOne);
        exceptionOne.initCause(exceptionTwo);
        exceptionTwo.initCause(exceptionThree);
        MetricsHandler.observeError(null, exceptionOne, null);
        String[] labels = {"UnrecognisedRuntimeError", "UnknownError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationWithCrossCausingExceptions()
    {
        Exception exceptionOne = new Exception();
        Exception exceptionTwo = new Exception(exceptionOne);
        exceptionOne.initCause(exceptionTwo);
        MetricsHandler.observeError(null, exceptionOne, null);
        String[] labels = {"UnrecognisedError", "UnknownError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationWithLoopingExceptionCause()
    {
        Exception exceptionOne = new Exception();
        Exception exceptionTwo = new Exception();
        Exception exceptionThree = new Exception(exceptionOne);
        exceptionOne.initCause(exceptionTwo);
        exceptionTwo.initCause(exceptionThree);
        MetricsHandler.observeError(null, exceptionOne, null);
        String[] labels = {"UnrecognisedError", "UnknownError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationWithNestedUniqueException()
    {
        RuntimeException nestedOtherErrorException = new RuntimeException(new java.net.SocketTimeoutException("socket timeout"));
        MetricsHandler.observeError(null, new Exception(nestedOtherErrorException), null);
        String[] labels = {"SocketTimeoutError", "OtherError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationToUserAuthenticationErrorWithExceptionOutlineMatching()
    {
        UnsupportedOperationException permissionsError = new UnsupportedOperationException("credentials issues");
        MetricsHandler.observeError(null, permissionsError, null);
        String[] labels = {"UnsupportedOperationError", "UserAuthenticationError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationToUserAuthenticationErrorWithTypeNameMatching()
    {
        MetricsHandler.observeError(null, new IllegalAccessException("user invalid details"), null);
        String[] labels = {"IllegalAccessError", "UserAuthenticationError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationToUserAuthenticationErrorWithKeywordsMatching()
    {
        MetricsHandler.observeError(null, new Exception("some text including kerberos keyword"), null);
        String[] labels = {"UnrecognisedError", "UserAuthenticationError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationToUserExecutionErrorWithExceptionOutlineMatching()
    {
        MetricsHandler.observeError(null, new Exception("some text including kerberos keyword"), null);
        String[] labels = {"UnrecognisedError", "UserAuthenticationError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Ignore("no type name patterns recorded for UserExecution error category yet")
    @Test
    public void testErrorCategorizationToUserExecutionErrorWithTypeNameMatching()
    {

    }

    @Test
    public void testErrorCategorizationToUserExecutionErrorWithKeywordsMatching()
    {
        MetricsHandler.observeError(null, new Exception("database schema 'someSchema' not found"), null);
        String[] labels = {"UnrecognisedError", "UserExecutionError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationToInternalServerErrorWithExceptionOutlineMatching()
    {
        MetricsHandler.observeError(null, new RuntimeException("something has not been configured properly"), null);
        String[] labels = {"UnrecognisedRuntimeError", "InternalServerError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationToInternalServerErrorWithTypeNameMatching()
    {
        MetricsHandler.observeError(null, new MissingFormatWidthException(""), null);
        String[] labels = {"MissingFormatWidthError", "InternalServerError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationToInternalServerErrorWithKeywordsMatching()
    {
        MetricsHandler.observeError(null, new Exception("overloaded servers"), null);
        String[] labels = {"UnrecognisedError", "InternalServerError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationToServerExecutionErrorWithExceptionOutlineMatching()
    {
        MetricsHandler.observeError(null, new IllegalArgumentException("there was an invalid hexadecimal representation of an ObjectId '123456789'"), null);
        String[] labels = {"IllegalArgumentError", "ServerExecutionError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);

        MetricsHandler.observeError(null, new EngineException("Error in 'some::graph': Can't find the profile 'some::profile'"), null);
        labels = new String[]{"UnrecognisedEngineError", "ServerExecutionError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationToServerExecutionErrorWithTypeNameMatching()
    {
        MetricsHandler.observeError(null, new JsonGenerationException("some message"), null);
        String[] labels = {"JsonGenerationError", "ServerExecutionError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationToServerExecutionErrorWithKeywordsMatching()
    {
        MetricsHandler.observeError(null, new Exception("Error in 'some::graph': Couldn't resolve test"), null);
        String[] labels = {"UnrecognisedError", "ServerExecutionError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationToOtherErrorWithExceptionOutlineMatching()
    {
        MetricsHandler.observeError(null, new IllegalArgumentException("Tests Failed! Error running tests for service 'some/service'"), null);
        String[] labels = {"IllegalArgumentError", "OtherError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationToOtherErrorWithTypeNameMatching()
    {
        MetricsHandler.observeError(null, new ConnectionClosedException(), null);
        String[] labels = {"ConnectionClosedError", "OtherError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationToOtherErrorWithKeywordsMatchingInExceptionName()
    {
        MetricsHandler.observeError(null, new TestCouldNotBeSkippedException(null), null);
        String[] labels = {"TestCouldNotBeSkippedError", "OtherError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationToOtherErrorWithKeywordsMatchingInExceptionMessage()
    {
        MetricsHandler.observeError(null, new Exception("some tests have failed!"), null);
        String[] labels = {"UnrecognisedError", "OtherError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationToUnknownErrorByAnyMatching()
    {
        MetricsHandler.observeError(null, new UnknownFormatFlagsException("some unknown error"), null);
        String[] labels = {"UnknownFormatFlagsError", "UnknownError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationMatchingMethodPrioritizationOfExceptionOutlineToKeywords()
    {
        MetricsHandler.observeError(LoggingEventType.DSB_EXECUTE_ERROR, new EngineException("Can't resolve the builder for function 'get/Login/Kerberos"), TEST_SERVICE_PATH);
        String[] labels = {"DsbExecuteEngineError", "ServerExecutionError", "DsbExecute", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationMatchingMethodPrioritizationOfKeywordsToTypeName()
    {
        MetricsHandler.observeError(null, new JsonGenerationException("can't get kerberos authentication"), TEST_SERVICE_PATH);
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
    public void testCorrectCounterSampleIncrementation()
    {
        MetricsHandler.observeError(null, new Exception(), null);
        MetricsHandler.observeError(null, new Exception(), null);
        String[] labels = {"UnrecognisedError", "UnknownError", "Unrecognised", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 2, DELTA);
    }

    @Test
    public void testErrorCategorizationExtractingValidCategoryFromEngineException()
    {
        MetricsHandler.observeError(null, new EngineException("some message", ErrorCategory.INTERNAL_SERVER_ERROR), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedEngineError", "InternalServerError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationExtractingUnknownCategoryFromEngineException()
    {
        MetricsHandler.observeError(null, new EngineException("Error in 'some::graph': Couldn't resolve test", ErrorCategory.UNKNOWN_ERROR), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedEngineError", "ServerExecutionError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationExtractingNullCategoryFromEngineException()
    {
        MetricsHandler.observeError(null, new EngineException("Error in 'some::graph': Couldn't resolve test", (ErrorCategory) null), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedEngineError", "ServerExecutionError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationExtractingValidCategoryFromNestedEngineException()
    {
        MetricsHandler.observeError(null, new Exception(new EngineException("some message", ErrorCategory.INTERNAL_SERVER_ERROR)), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedEngineError", "InternalServerError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationMatchingTechniquePrioritizationOfCategorisedExceptionToMatching()
    {
        MetricsHandler.observeError(null, new Exception("kerberos", new EngineException("some message", ErrorCategory.INTERNAL_SERVER_ERROR)), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedEngineError", "InternalServerError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorCategorizationRegexCaseInsensitivity()
    {
        MetricsHandler.observeError(null, new Exception("KERBEROS"), TEST_SERVICE_PATH);
        String[] labels = {"UnrecognisedError", "UserAuthenticationError", "Unrecognised", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, ERROR_LABEL_NAMES, labels), 1, DELTA);
    }
}
