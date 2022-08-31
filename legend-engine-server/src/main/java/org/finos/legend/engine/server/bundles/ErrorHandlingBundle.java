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


package org.finos.legend.engine.server.bundles;

import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.finos.legend.engine.server.core.configuration.ErrorHandlingConfiguration;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;

import java.util.function.Function;

public abstract class ErrorHandlingBundle<T> implements ConfiguredBundle<T>
{
    public ErrorHandlingBundle()
    {
    }

    @Override
    public void run(T serverConfiguration, Environment environment) throws Exception
    {
        ErrorHandlingConfiguration errorHandlingConfiguration = this.getErrorHandlingConfiguration((serverConfiguration));
        MetricsHandler.setDoExceptionCategorisation(errorHandlingConfiguration.doExceptionCategorisation);
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap)
    {
    }

    protected abstract ErrorHandlingConfiguration getErrorHandlingConfiguration(T configuration);
}
