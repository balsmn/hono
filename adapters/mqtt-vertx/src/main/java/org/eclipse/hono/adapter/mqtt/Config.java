/**
 * Copyright (c) 2016, 2017 Red Hat and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat - initial creation
 *    Bosch Software Innovations GmbH
 */

package org.eclipse.hono.adapter.mqtt;

import org.eclipse.hono.config.ApplicationConfigProperties;
import org.eclipse.hono.config.ClientConfigProperties;
import org.eclipse.hono.config.ServiceConfigProperties;
import org.eclipse.hono.service.AbstractAdapterConfig;
import org.springframework.beans.factory.config.ObjectFactoryCreatingFactoryBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Configuration class
 */
@Configuration
public class Config extends AbstractAdapterConfig {

    private static final String BEAN_NAME_VERTX_BASED_MQTT_PROTOCOL_ADAPTER = "vertxBasedMqttProtocolAdapter";

    @Bean(name = BEAN_NAME_VERTX_BASED_MQTT_PROTOCOL_ADAPTER)
    @Scope("prototype")
    public VertxBasedMqttProtocolAdapter vertxBasedMqttProtocolAdapter(){
        return new VertxBasedMqttProtocolAdapter();
    }

    @Override
    protected void customizeMessagingClientConfigProperties(final ClientConfigProperties props) {
        if (props.getName() == null) {
            props.setName("Hono MQTT Adapter");
        }
    }

    @Override
    protected void customizeRegistrationServiceClientConfigProperties(ClientConfigProperties props) {
        if (props.getName() == null) {
            props.setName("Hono MQTT Adapter");
        }
    }

    /**
     * Exposes properties for configuring the application properties a Spring bean.
     *
     * @return The application configuration properties.
     */
    @Bean
    @ConfigurationProperties(prefix = "hono.app")
    public ApplicationConfigProperties applicationConfigProperties(){
        return new ApplicationConfigProperties();
    }

    /**
     * Exposes the MQTT adapter's configuration properties as a Spring bean.
     * 
     * @return The configuration properties.
     */
    @Bean
    @ConfigurationProperties(prefix = "hono.mqtt")
    public ServiceConfigProperties adapterProperties() {
        return new ServiceConfigProperties();
    }

    /**
     * Exposes a factory for creating MQTT adapter instances.
     * 
     * @return The factory bean.
     */
    @Bean
    public ObjectFactoryCreatingFactoryBean serviceFactory() {
        ObjectFactoryCreatingFactoryBean factory = new ObjectFactoryCreatingFactoryBean();
        factory.setTargetBeanName(BEAN_NAME_VERTX_BASED_MQTT_PROTOCOL_ADAPTER);
        return factory;
    }
}
