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

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ErrorCategory;
import org.finos.legend.engine.shared.core.operational.errorManagement.ErrorOrigin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

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

    // -------------------------------------- ERROR HANDLING -------------------------------------

    /**
     * Path to JSON file outlining error data for categorisation available to open source
     */
    private static final String EXTERNAL_ERROR_DATA_PATH = "/ErrorData.json";

    /**
     * Path to JSON file outlining error data for categorisation to use internally
     */
    private static final String INTERNAL_ERROR_DATA_DIR = "com.gs.alloy.engine.server.full.Server";

    /**
     * Prometheus counter to record errors with labels of the service causing the error if it is a service-related error,
     * the label given to the error, the category of the error and source of the error
     */
    private static final Counter ERROR_COUNTER = Counter.build("legend_engine_error_total", "Count errors in legend ecosystem").labelNames("errorLabel", "category", "source", "serviceName").register(getMetricsRegistry());

    /**
     * User friendly error categories
     */
    private enum ERROR_CATEGORIES
    { UserAuthenticationError, UserExecutionError, InternalServerError, ServerExecutionError, OtherError, UnknownError }

    /**
     * Types of error matching techniques that can be performed on an incoming exceptions
     */
    public enum MATCHING_METHODS
    { ExceptionOutlineMatching, KeywordsMatching, TypeNameMatching }

    /**
     * List of objects corresponding to the error categories holding their associated exception data
     */
    private static final ArrayList<ErrorCategory> ERROR_CATEGORY_DATA_OBJECTS = readErrorData();

    /**
     * Method to obtain a label for the error that has occurred - Mostly converts exception class name directly to label except:
     * RuntimeException, Exception and EngineExceptions which are further processed and often combined with their origin value.
     * @param origin the stage in execution at which the error occurred
     * @param exception the exception to be analysed that has occurred in execution
     * @return the error label generated for the error
     */
    private static synchronized String getErrorLabel(String origin, Exception exception)
    {
        String errorName = exception.getClass().getSimpleName();
        HashSet<Exception> exploredExceptions = new HashSet<>();
        String[] genericExceptionNames = { RuntimeException.class.getSimpleName(), Exception.class.getSimpleName(), EngineException.class.getSimpleName() };
        while (Arrays.asList(genericExceptionNames).contains(errorName) && exception.getCause() != null && exception.getCause() instanceof Exception && !exploredExceptions.contains(exception.getCause()))
        {
            exploredExceptions.add(exception);
            exception = (Exception) exception.getCause();
            errorName = exception.getClass().getSimpleName();
        }
        if (errorName.equals(RuntimeException.class.getSimpleName()) || errorName.equals(Exception.class.getSimpleName()))
        {
            errorName = origin + errorName;
        }
        else if (errorName.equals(EngineException.class.getSimpleName()))
        {
            errorName = ((EngineException) exception).getErrorType() != null ? ((EngineException) exception).getErrorType().toString().toLowerCase() + errorName : origin + errorName;
        }
        return (errorName.substring(0,1).toUpperCase() + errorName.substring(1)).substring(0, errorName.lastIndexOf("Exception")) + "Error";
    }

    /**
     * Method to record an error occurring during execution and add it to the metrics
     * @param origin the stage in execution at which the error occurred
     * @param exception the non-null exception to be analysed that has occurred in execution
     * @param servicePath the name of the service whose execution invoked the error
     */
    public static synchronized void observeError(ErrorOrigin origin, Exception exception, String servicePath)
    {
        origin = origin == null ? ErrorOrigin.UNRECOGNISED : origin;
        String errorLabel = getErrorLabel(origin.toFriendlyString(), exception);
        String source = servicePath == null ? origin.toFriendlyString() : "Service";
        String servicePattern = servicePath == null ? "N/A" : servicePath;
        String errorCategory = getErrorCategory(exception).toString();
        ERROR_COUNTER.labels(errorLabel, errorCategory, source, servicePattern).inc();
    }

    /**
     * Method to categorise the exception that has occurred
     * If original exception cannot be matched its cause is attempted to be matched until the cause is null
     * @param exception the exception to be analysed that has occurred in execution
     * @return the user-friendly error category
     */
    private static synchronized ERROR_CATEGORIES getErrorCategory(Exception exception)
    {
        Exception originalException = exception;
        HashSet<Exception> exceptionHistory = new HashSet();
        while (exception != null && !exceptionHistory.contains(exception))
        {
            for (MATCHING_METHODS method : MATCHING_METHODS.values())
            {
                for (ErrorCategory category : ERROR_CATEGORY_DATA_OBJECTS)
                {
                    if (category.matches(exception, method))
                    {
                        return ERROR_CATEGORIES.valueOf(category.getFriendlyName());
                    }
                }
            }
            exceptionHistory.add(exception);
            exception = exception.getCause() != null && exception.getCause() instanceof Exception ? (Exception) exception.getCause() : null;
        }
        LOGGER.warn(String.format("Unknown error - Exception Name: %s. Exception Message: %s. Add it to ErrorData.json!", originalException.getClass().getSimpleName(), originalException.getMessage()));
        return ERROR_CATEGORIES.UnknownError;
    }

    /**
     * Find and read JSON file with outline of errors to be used in categorizing the exceptions
     * @return List of objects corresponding to the error categories with their respective data
     */
    private static synchronized ArrayList<ErrorCategory> readErrorData()
    {
        ArrayList<ErrorCategory> categories = new ArrayList<>();
        JSONParser jsonParser = new JSONParser();
        try (InputStream inputStream = MetricsHandler.class.getResourceAsStream(INTERNAL_ERROR_DATA_DIR))
        {
            JSONObject errorData = (JSONObject) jsonParser.parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            categories = parseErrorData(errorData);
            LOGGER.info("Successfully read internal error data categorisation file");
        }
        catch (Exception e)
        {
            LOGGER.info("Could not read internal error data file - attempting to read external version");
            try (InputStream inputStream = MetricsHandler.class.getResourceAsStream(EXTERNAL_ERROR_DATA_PATH))
            {
                JSONObject errorData = (JSONObject) jsonParser.parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                categories = parseErrorData(errorData);
                LOGGER.info("Successfully read external error data categorisation file");
            }
            catch (Exception exception)
            {
                LOGGER.error(String.format("Error reading exception categorisation data: %s - could not read external file either", exception));
            }
        }
        return categories;
    }

    /**
     * Method to parse the read JSON Object and initialize ErrorCategory objects from it
     * @param errorData the JSON Object read from the file to be converted to ErrorCategory objects
     * @return List of objects corresponding to the error categories with their respective data
     */
    private static synchronized ArrayList<ErrorCategory> parseErrorData(JSONObject errorData)
    {
        ArrayList<ErrorCategory> categories = new ArrayList<>();
        JSONArray errorCategories = (JSONArray) errorData.get("ErrorCategories");
        for (Object errorCategoryData : errorCategories)
        {
            ErrorCategory category = new ErrorCategory((JSONObject) errorCategoryData);
            categories.add(category);
        }
        return categories;
    }

    /**
     * Accessor for the Prometheus Collector for errors
     * @return Counter measuring error frequency
     */
    public static Counter getErrorCounter()
    {
        return ERROR_COUNTER;
    }

}
