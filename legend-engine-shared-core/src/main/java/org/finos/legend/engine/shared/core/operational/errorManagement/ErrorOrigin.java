package org.finos.legend.engine.shared.core.operational.errorManagement;

public enum ErrorOrigin {
    PURE_QUERY_EXECUTION,
    GENERATE_PLAN,
    LAMBDA_RETURN_TYPE,

    COMPILE_MODEL,
    MODEL_RESOLVE,
    
    SERVICE_TEST_EXECUTE,
    SERVICE_EXECUTE,


    TDS_PROTOCOL,
    TDS_EXECUTE,
    TDS_GENERATE_CODE,
    TDS_SCHEMA,
    TDS_LAMBDA,
    TDS_METADATA,
    TDS_INPUTS,

    DSB_EXECUTE,


}
