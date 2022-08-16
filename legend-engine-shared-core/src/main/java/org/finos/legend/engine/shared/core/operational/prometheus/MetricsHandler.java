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

package org.finos.legend.engine.shared.core.operational.prometheus;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ErrorCategory;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class MetricsHandler
{
    public static final String METRIC_PREFIX = "alloy_";
    private static final String[] empty = new String[]{};
    static MutableMap<String, Summary> serviceMetrics = Maps.mutable.empty();
    static MutableMap<String, Gauge> gauges = Maps.mutable.empty();
    static final Gauge allExecutions = Gauge.build().name("alloy_executions").help("Execution gauge metric ").register();

    // ----------------------------------------- NEW IMPLEMENTATION -----------------------------------------

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsHandler.class);

    private static final CollectorRegistry METRICS_REGISTRY = new CollectorRegistry(true);

    private static final Histogram SUCCESSFUL_REQUEST_LATENCY = Histogram.build().name("legend_engine_successful_request_latency")
            .help("Measure legend engine's http request latency")
            .buckets(.1, .2, .5, 1, 2, 5, 10, 30, 100, 300)
            .labelNames("uri")
            .register(getMetricsRegistry());

    private static final Histogram OPERATION_LATENCY = Histogram.build().name("legend_operation_latency")
            .help("Measure particular operation latency within legend ecosystem")
            .buckets(.1, .2, .5, 1, 2, 5, 10, 20, 60, 120)
            .labelNames("operation", "context")
            .register(getMetricsRegistry());

    private static final Counter ALL_EXECUTIONS = Counter.build("legend_engine_executions", "Execution counter metric ").register();
    private static final Counter DATASTORE_SPEC_COUNT = Counter.build("legend_engine_datastore_spec_count", "Count datastore specifications").register(getMetricsRegistry());
    private static final Counter JAVA_COMPILATION_COUNT = Counter.build("legend_engine_java_compilation_count", "Count java compilations").register(getMetricsRegistry());
    private static final Gauge TEMP_FILE_COUNT = Gauge.build("legend_engine_temp_file_count", "Measure how many temporary files are being currently created").register(getMetricsRegistry());

    public static CollectorRegistry getMetricsRegistry()
    {
        return METRICS_REGISTRY;
    }

    public static void observeRequest(String uri, long start, long end)
    {
        if (uri == null)
        {
            LOGGER.warn("Metric cannot be saved. Uri or method label values are missing");
        }
        else
        {
            SUCCESSFUL_REQUEST_LATENCY.labels(uri).observe((end - start) / 1000F);
        }
    }

    public static void observeServerOperation(String operation, String context, long start, long end)
    {
        observeOperation(operation, context, start, end);
    }

    private static void observeOperation(String operation, String context, long start, long end)
    {
        if (operation == null)
        {
            LOGGER.warn("Metric cannot be saved. Operation label value is missing");
        }
        else
        {
            OPERATION_LATENCY.labels(operation, returnLabelOrUnknown(context)).observe((end - start) / 1000F);
        }
    }

    public static void incrementExecutionCount()
    {
        ALL_EXECUTIONS.inc();
    }

    public static void incrementDatastoreSpecCount()
    {
        DATASTORE_SPEC_COUNT.inc();
    }

    public static void incrementJavaCompilationCount()
    {
        JAVA_COMPILATION_COUNT.inc();
    }

    public static void incrementTempFileCount()
    {
        TEMP_FILE_COUNT.inc();
    }

    public static void decrementTempFileCount()
    {
        TEMP_FILE_COUNT.dec();
    }

    private static String returnLabelOrUnknown(String label)
    {
        return label != null ? label : "unknown";
    }

    // -------------------------------------- END OF NEW IMPLEMENTATION -------------------------------------

    @Deprecated
    public static <T> void createMetrics(Class<T> c)
    {
        for (Method m : c.getMethods())
        {
            if (m.isAnnotationPresent(Prometheus.class))
            {
                Prometheus val = m.getAnnotation(Prometheus.class);
                if (val.type() == Prometheus.Type.SUMMARY && (serviceMetrics.get(val.name()) == null))
                {
                    Summary g = Summary.build().name(generateMetricName(val.name(), false))
                            .quantile(0.5, 0.05).quantile(0.9, 0.01).quantile(0.99, 0.001)
                            .help(val.doc())
                            .register();
                    serviceMetrics.put(val.name(), g);
                }
            }
        }
    }

    @Deprecated
    public static void incrementExecutionGauge()
    {
        allExecutions.inc();
    }

    @Deprecated
    public static synchronized void observe(String name, long startTime, long endTime)
    {
        if (serviceMetrics.get(name) == null)
        {
            Summary g = Summary.build().name(generateMetricName(name, false))
                    .quantile(0.5, 0.05).quantile(0.9, 0.01).quantile(0.99, 0.001)
                    .help(name + " duration metrics")
                    .register();
            serviceMetrics.put(name, g);
            g.observe((endTime - startTime) / 1000F);
        }
        else
        {
            serviceMetrics.get(name).observe((endTime - startTime) / 1000F);
        }
    }

    @Deprecated
    public static synchronized void observeCount(String name)
    {
        observeCount(name, empty, empty, false);
    }

    @Deprecated
    public static synchronized void decrementCount(String name)
    {
        observeCount(name, empty, empty, true);
    }

    @Deprecated
    public static synchronized void observeCount(String name, String[] labelNames, String[] labelValues, boolean decrement)
    {
        Gauge g;
        if (gauges.get(name) == null)
        {
            g = Gauge.build().name(generateMetricName(name, false))
                    .help(name + " gauge metric")
                    .labelNames(labelNames).register();
            gauges.put(name, g);
            if (decrement)
            {
                g.labels(labelValues).dec();
            }
            else
            {
                g.labels(labelValues).inc();
            }
        }
        else
        {
            g = gauges.get(name);
            if (decrement)
            {
                g.labels(labelValues).dec();
            }
            else
            {
                g.labels(labelValues).inc();
            }
        }
    }

    // -------------------------------------- ERROR HANDLING -------------------------------------

    /**
     * Path to JSON file outlining error data for categorisation available to open source.
     */
    private static final String ERROR_DATA_PATH = "/ErrorData.json";

    /**
     * Prometheus counter to record errors with labels of the service causing the error if it is a service-related error,
     * the label given to the error, the category of the error and source of the error.
     */
    protected static final Counter ERROR_COUNTER = Counter.build("legend_engine_error_total", "Count errors in legend ecosystem").labelNames("errorLabel", "category", "source", "serviceName").register(getMetricsRegistry());

    /**
     * Types of error matching techniques that can be performed on an incoming exceptions.
     */
    public enum MatchingMethod
    { EXCEPTION_OUTLINE_MATCHING, KEYWORDS_MATCHING, TYPE_NAME_MATCHING }

    /**
     * List of objects corresponding to the error categories holding their associated exception data.
     */
    private static final List<ExceptionCategory> ERROR_CATEGORY_DATA_OBJECTS = readErrorData();

    /**
     * Method to record an error occurring during execution and add it to the metrics.
     * @param origin the stage in execution at which the error occurred.
     * @param exception the non-null exception to be analysed that has occurred in execution.
     * @param servicePath the name of the service whose execution invoked the error.
     */
    public static synchronized void observeError(Enum origin, Exception exception, String servicePath)
    {
        origin = origin == null ? LoggingEventType.UNRECOGNISED_ERROR : origin;
        String errorLabel = getErrorLabel(removeErrorSuffix(toCamelCase(origin)), exception);
        String source = servicePath == null ? toCamelCase(origin) : toCamelCase(LoggingEventType.SERVICE_EXECUTE_ERROR);
        source = removeErrorSuffix(source);
        String servicePattern = servicePath == null ? "N/A" : servicePath;
        String errorCategory = toCamelCase(getErrorCategory(exception));
        ERROR_COUNTER.labels(errorLabel, errorCategory, source, servicePattern).inc();
        LOGGER.info("Error added to metric - Label: {}. Category: {}. Source: {}. Service: {}. {}.", errorLabel, errorCategory, source, servicePattern, exceptionToPrettyString(exception));
    }

    /**
     * Method to obtain a label for the error that has occurred - Mostly converts exception class name directly to label except:
     * RuntimeException, Exception and EngineExceptions which are further processed and often combined with their origin value.
     * @param origin the stage in execution at which the error occurred.
     * @param exception the exception to be analysed that has occurred in execution.
     * @return the error label generated for the error.
     */
    private static synchronized String getErrorLabel(String origin, Throwable exception)
    {
        Class errorClass = exception.getClass();
        HashSet<Throwable> exploredExceptions = new HashSet<>();
        Class[] genericExceptionClasses = { RuntimeException.class, Exception.class, EngineException.class };
        while (Arrays.asList(genericExceptionClasses).contains(errorClass) && exception.getCause() != null && !exploredExceptions.contains(exception.getCause()))
        {
            exploredExceptions.add(exception);
            exception = exception.getCause();
            errorClass = exception.getClass();
        }
        String errorLabel = exception.getClass().getSimpleName();
        if (errorClass.equals(RuntimeException.class) || errorClass.equals(Exception.class))
        {
            errorLabel = origin + errorClass.getSimpleName();
        }
        else if (exception instanceof EngineException)
        {
            errorLabel = ((EngineException) exception).getErrorType() != null ? ((EngineException) exception).getErrorType().toString().toLowerCase() + errorClass.getSimpleName() : origin + errorClass.getSimpleName();
        }
        return convertErrorLabelToPrettyString(errorLabel);
    }

    /**
     * Method to delegate obtaining the error category from an exception either by matching or extracting from EngineException.
     * @param exception the exception to be analysed that has occurred in execution.
     * @return the user-friendly error category.
     */
    private static synchronized ErrorCategory getErrorCategory(Throwable exception)
    {
        ErrorCategory engineExceptionCategory = tryExtractErrorCategoryFromEngineException(exception);
        return engineExceptionCategory == ErrorCategory.UNKNOWN_ERROR ? tryMatchExceptionToErrorDataFile(exception) : engineExceptionCategory;
    }

    /**
     * Method to try and match an exception to an error category using the matching methods.
     * If an initial exception can't be matched its cause it matched.
     * @param exception is the original exception that occurred in the engine.
     * @return Error category belonging to the exception.
     */
    private static synchronized ErrorCategory tryMatchExceptionToErrorDataFile(Throwable exception)
    {
        HashSet<Throwable> exceptionHistory = new HashSet();
        while (exception != null && !exceptionHistory.contains(exception))
        {
            for (MatchingMethod method : MatchingMethod.values())
            {
                for (ExceptionCategory category : ERROR_CATEGORY_DATA_OBJECTS)
                {
                    if (category.matches(exception, method))
                    {
                        return category.getErrorCategory();
                    }
                }
            }
            exceptionHistory.add(exception);
            exception = exception.getCause();
        }
        return ErrorCategory.UNKNOWN_ERROR;
    }

    /**
     * Method to try and get an error category from a possible engine exception in the original exception's trace.
     * If the original exception is not an EngineException or does not have its category field populated the cause is analysed.
     * @param exception is the original exception that occurred in the engine.
     * @return Error category belonging to the exception or UnknownError if no meaningful data could be obtained from a possible EngineException.
     */
    private static synchronized ErrorCategory tryExtractErrorCategoryFromEngineException(Throwable exception)
    {
        HashSet<Throwable> exceptionHistory = new HashSet();
        while (exception != null && !exceptionHistory.contains(exception))
        {
            if (exception instanceof EngineException)
            {
                EngineException engineException = (EngineException) exception;
                if (engineException.getErrorCategory() != null && engineException.getErrorCategory() != ErrorCategory.UNKNOWN_ERROR)
                {
                    return engineException.getErrorCategory();
                }
            }
            exceptionHistory.add(exception);
            exception = exception.getCause();
        }
        return ErrorCategory.UNKNOWN_ERROR;
    }

    /**
     * Find and read JSON file with outline of errors to be used in categorizing the exceptions
     * @return List of objects corresponding to the error categories with their respective data
     */
    private static synchronized List<ExceptionCategory> readErrorData()
    {
        List<ExceptionCategory> categories;
        try (InputStream inputStream = MetricsHandler.class.getResourceAsStream(ERROR_DATA_PATH))
        {
            categories = Arrays.asList(new ObjectMapper().readValue(inputStream, ExceptionCategory[].class));
            LOGGER.info("Successfully read error data from {}.", MetricsHandler.class.getResource(ERROR_DATA_PATH));
        }
        catch (Exception e)
        {
                LOGGER.warn("Error reading exception categorisation data: {}", exceptionToPrettyString(e));
                EngineException engineException = new EngineException("Cannot read error data file properly", e, ErrorCategory.INTERNAL_SERVER_ERROR);
                observeError(LoggingEventType.ERROR_MANAGEMENT_ERROR, engineException, null);
                throw engineException;
        }
        return categories;
    }

    // -------------------------------------- STRING UTILS -------------------------------------

    /**
     * Method to convert a snake case enum value to camel case for pretty printing for metrics
     * @param value NonNull enum value to be converted
     * @return camelCase string of enum value
     */
    public static String toCamelCase(Enum value)
    {
        String snakeCaseString = value.toString();
        String[] elements = snakeCaseString.toLowerCase().split("_");
        StringBuilder output = new StringBuilder();
        Arrays.stream(elements).forEach(element -> output.append(element.substring(0, 1).toUpperCase()).append(element.substring(1)));
        return output.toString();
    }

    /**
     * Method to delete the suffix "Error" to a camel case string if it exists.
     * @param string the string whose suffix to remove
     * @return string without the suffix "Error" if applicable.
     */
    private static String removeErrorSuffix(String string)
    {
        return string.endsWith("Error") ? string.substring(0, string.indexOf("Error")) : string;
    }

    /**
     * Method to take a label and change it to a pretty printed string by capitalising the first letter
     * and replacing 'Exception' with 'Error'
     * @param errorLabel is the string to be pretty printed
     * @return pretty print version of error label for error metrics
     */
    private static String convertErrorLabelToPrettyString(String errorLabel)
    {
        String capitalisedErrorLabel = errorLabel.substring(0,1).toUpperCase() + errorLabel.substring(1);
        String labelWithRemovedWord = capitalisedErrorLabel.substring(0, capitalisedErrorLabel.lastIndexOf("Exception"));
        return removeErrorSuffix(labelWithRemovedWord) + "Error";
    }

    /**
     * Method to pretty print an exception with the purpose of adding it to the error data file
     * @param exception is the exception to be pretty printed
     * @return pretty print formatted exception string
     */
    private static String exceptionToPrettyString(Exception exception)
    {
        String name = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        String cause = exception.getCause() == null ? "None" : exception.getCause().toString();
        return String.format("Exception: %s. Message: %s. Cause: %s", name, message, cause);
    }

    @Deprecated
    public static String generateMetricName(String name, boolean isErrorMetric)
    {
        return METRIC_PREFIX + name
                .replace("/", "_")
                .replace("-", "_")
                .replace("{", "")
                .replace("}", "")
                .replaceAll(" ", "_") + (isErrorMetric ? "_errors" : "");
    }
}
