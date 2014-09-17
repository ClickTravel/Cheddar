/*
 * Copyright 2014 Click Travel Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.clicktravel.cheddar.server.runtime.config;

import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

public abstract class PropertiesConfigurationBuilder {

    public static PropertySourcesPlaceholderConfigurer configurer(final String servicePropertiesName,
            final Environment environment) {
        final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        final ClassPathResource envProperties = new ClassPathResource("com.clicktravel.services.env.properties");
        final ClassPathResource serverProperties = new ClassPathResource("com.clicktravel.cheddar.server.properties");
        final ClassPathResource serviceProperties = new ClassPathResource(
                (RuntimeConfiguration.isLocalEnvironment(environment) ? "local-" : "") + servicePropertiesName);
        // Note : Later property resources in this array override earlier ones
        configurer.setLocations(new ClassPathResource[] { serviceProperties, serverProperties, envProperties });
        return configurer;
    }
}