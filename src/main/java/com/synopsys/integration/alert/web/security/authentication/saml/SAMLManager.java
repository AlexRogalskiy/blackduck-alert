/**
 * blackduck-alert
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.alert.web.security.authentication.saml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLBootstrap;
import org.springframework.security.saml.SAMLEntryPoint;
import org.springframework.security.saml.SAMLLogoutFilter;
import org.springframework.security.saml.SAMLLogoutProcessingFilter;
import org.springframework.security.saml.SAMLProcessingFilter;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.key.EmptyKeyManager;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.log.SAMLDefaultLogger;
import org.springframework.security.saml.metadata.CachingMetadataManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataGeneratorFilter;
import org.springframework.security.saml.parser.ParserPoolHolder;
import org.springframework.security.saml.processor.HTTPPostBinding;
import org.springframework.security.saml.processor.HTTPRedirectDeflateBinding;
import org.springframework.security.saml.processor.SAMLBinding;
import org.springframework.security.saml.processor.SAMLProcessorImpl;
import org.springframework.security.saml.util.VelocityFactory;
import org.springframework.security.saml.websso.SingleLogoutProfile;
import org.springframework.security.saml.websso.SingleLogoutProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfile;
import org.springframework.security.saml.websso.WebSSOProfileConsumer;
import org.springframework.security.saml.websso.WebSSOProfileConsumerHoKImpl;
import org.springframework.security.saml.websso.WebSSOProfileConsumerImpl;
import org.springframework.security.saml.websso.WebSSOProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfileOptions;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.synopsys.integration.alert.common.exception.AlertDatabaseConstraintException;
import com.synopsys.integration.alert.common.exception.AlertLDAPConfigurationException;
import com.synopsys.integration.alert.common.persistence.accessor.ConfigurationAccessor;
import com.synopsys.integration.alert.common.persistence.model.ConfigurationFieldModel;
import com.synopsys.integration.alert.common.persistence.model.ConfigurationModel;
import com.synopsys.integration.alert.component.settings.SettingsDescriptor;

@EnableWebSecurity
@Configuration
@Profile("saml")
public class SAMLManager extends WebSecurityConfigurerAdapter {
    public static final String SSO_PROVIDER_NAME = "Synopsys - Alert";

    private static final Logger logger = LoggerFactory.getLogger(SAMLManager.class);
    private static final String[] DEFAULT_PATHS = {
        "/saml/**",
        "/#",
        "/favicon.ico",
        "/fonts/**",
        "/js/bundle.js",
        "/js/bundle.js.map",
        "/css/style.css"
    };

    private final HttpSessionCsrfTokenRepository csrfTokenRepository;
    private final ConfigurationAccessor configurationAccessor;

    @Autowired
    public SAMLManager(final HttpSessionCsrfTokenRepository csrfTokenRepository, final ConfigurationAccessor configurationAccessor) {
        this.csrfTokenRepository = csrfTokenRepository;
        this.configurationAccessor = configurationAccessor;
    }

    public String[] getAllowedPaths() {
        return DEFAULT_PATHS;
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(samlAuthenticationProvider());
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http.exceptionHandling().authenticationEntryPoint(samlEntryPoint());

        http.csrf().csrfTokenRepository(csrfTokenRepository).ignoringAntMatchers(getAllowedPaths());

        http.addFilterBefore(metadataGeneratorFilter(), ChannelProcessingFilter.class)
            .addFilterAfter(samlFilter(), BasicAuthenticationFilter.class);

        http.authorizeRequests().antMatchers(getAllowedPaths()).permitAll()
            .anyRequest().authenticated();

        http.logout().logoutSuccessUrl("/");
    }

    public ConfigurationModel getCurrentConfiguration() throws AlertDatabaseConstraintException, AlertLDAPConfigurationException {
        return configurationAccessor.getConfigurationsByDescriptorName(SettingsDescriptor.SETTINGS_COMPONENT)
                   .stream()
                   .findFirst()
                   .orElseThrow(() -> new AlertLDAPConfigurationException("Settings configuration missing"));
    }

    public boolean isSAMLEnabled() {
        boolean enabled = false;
        try {
            enabled = Boolean.valueOf(getFieldValueOrEmpty(getCurrentConfiguration(), SettingsDescriptor.KEY_SAML_ENABLED));
        } catch (final AlertDatabaseConstraintException | AlertLDAPConfigurationException ex) {
            logger.warn(ex.getMessage());
            logger.debug("cause: ", ex);
        }

        return enabled;
    }

    private String getFieldValueOrEmpty(final ConfigurationModel configurationModel, final String fieldKey) {
        return configurationModel.getField(fieldKey).flatMap(ConfigurationFieldModel::getFieldValue).orElse("");
    }

    private Boolean getFieldValueBoolean(final ConfigurationModel configurationModel, final String fieldKey) {
        return configurationModel.getField(fieldKey).flatMap(ConfigurationFieldModel::getFieldValue).map(BooleanUtils::toBoolean).orElse(false);
    }

    @Lazy
    @Bean
    public SAMLEntryPoint samlEntryPoint() throws AlertDatabaseConstraintException, AlertLDAPConfigurationException {
        final SAMLEntryPoint samlEntryPoint = new SAMLEntryPoint();

        final WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();
        webSSOProfileOptions.setIncludeScoping(false);
        webSSOProfileOptions.setProviderName(SSO_PROVIDER_NAME);
        webSSOProfileOptions.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        final Boolean forceAuth = getFieldValueBoolean(getCurrentConfiguration(), SettingsDescriptor.KEY_SAML_FORCE_AUTH);
        webSSOProfileOptions.setForceAuthN(forceAuth);

        samlEntryPoint.setDefaultProfileOptions(webSSOProfileOptions);
        return samlEntryPoint;
    }

    @Bean
    public SAMLProcessingFilter samlWebSSOProcessingFilter() throws Exception {
        final SAMLProcessingFilter samlWebSSOProcessingFilter = new SAMLProcessingFilter();
        samlWebSSOProcessingFilter.setAuthenticationManager(authenticationManager());

        final SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler =
            new SavedRequestAwareAuthenticationSuccessHandler();
        successRedirectHandler.setDefaultTargetUrl("/");
        samlWebSSOProcessingFilter.setAuthenticationSuccessHandler(successRedirectHandler);

        samlWebSSOProcessingFilter.setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler());
        return samlWebSSOProcessingFilter;
    }

    @Bean
    public SimpleUrlLogoutSuccessHandler successLogoutHandler() {
        final SimpleUrlLogoutSuccessHandler simpleUrlLogoutSuccessHandler =
            new SimpleUrlLogoutSuccessHandler();
        simpleUrlLogoutSuccessHandler.setDefaultTargetUrl("/");
        simpleUrlLogoutSuccessHandler.setAlwaysUseDefaultTargetUrl(true);
        return simpleUrlLogoutSuccessHandler;
    }

    @Lazy
    @Bean
    public MetadataGeneratorFilter metadataGeneratorFilter() throws AlertDatabaseConstraintException, AlertLDAPConfigurationException {
        final MetadataGenerator metadataGenerator = new MetadataGenerator();
        final ConfigurationModel currentConfiguration = getCurrentConfiguration();
        final String entityId = getFieldValueOrEmpty(currentConfiguration, SettingsDescriptor.KEY_SAML_ENTITY_ID);
        metadataGenerator.setEntityId(entityId);
        final String entityURL = getFieldValueOrEmpty(currentConfiguration, SettingsDescriptor.KEY_SAML_ENTITY_BASE_URL);
        metadataGenerator.setEntityBaseURL(entityURL);
        metadataGenerator.setExtendedMetadata(extendedMetadata());
        metadataGenerator.setIncludeDiscoveryExtension(false);
        metadataGenerator.setKeyManager(keyManager());
        metadataGenerator.setRequestSigned(false);
        metadataGenerator.setWantAssertionSigned(false);
        metadataGenerator.setBindingsSLO(Collections.emptyList());
        metadataGenerator.setBindingsSSO(Arrays.asList("post"));
        metadataGenerator.setNameID(Arrays.asList(NameIDType.UNSPECIFIED));

        return new MetadataGeneratorFilter(metadataGenerator);
    }

    @Bean
    public KeyManager keyManager() {
        return new EmptyKeyManager();
    }

    @Bean
    public ExtendedMetadata extendedMetadata() {
        final ExtendedMetadata extendedMetadata = new ExtendedMetadata();
        extendedMetadata.setIdpDiscoveryEnabled(false);
        extendedMetadata.setSignMetadata(false);
        extendedMetadata.setEcpEnabled(true);
        extendedMetadata.setRequireLogoutRequestSigned(false);
        return extendedMetadata;
    }

    @Bean
    public FilterChainProxy samlFilter() throws Exception {
        final List<SecurityFilterChain> chains = new ArrayList<>();

        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/login/**"), samlEntryPoint()));

        final SAMLProcessingFilter samlProcessingFilter = samlWebSSOProcessingFilter();
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSO/**"), samlProcessingFilter));

        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/logout/**"), samlLogoutFilter()));

        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SingleLogout/**"), samlLogoutProcessingFilter()));

        return new FilterChainProxy(chains);
    }

    @Bean
    public SecurityContextLogoutHandler logoutHandler() {
        final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setInvalidateHttpSession(true);
        logoutHandler.setClearAuthentication(true);
        return logoutHandler;
    }

    @Bean
    public SAMLLogoutFilter samlLogoutFilter() {
        return new SAMLLogoutFilter(successLogoutHandler(),
            new LogoutHandler[] { logoutHandler() },
            new LogoutHandler[] { logoutHandler() });
    }

    @Bean
    public SAMLLogoutProcessingFilter samlLogoutProcessingFilter() {
        return new SAMLLogoutProcessingFilter(successLogoutHandler(), logoutHandler());
    }

    @Bean
    public VelocityEngine velocityEngine() {
        return VelocityFactory.getEngine();
    }

    @Bean(initMethod = "initialize")
    public StaticBasicParserPool parserPool() {
        return new StaticBasicParserPool();
    }

    @Bean(name = "parserPoolHolder")
    public ParserPoolHolder parserPoolHolder() {
        return new ParserPoolHolder();
    }

    @Bean
    public HTTPPostBinding httpPostBinding() {
        return new HTTPPostBinding(parserPool(), velocityEngine());
    }

    @Bean
    public HTTPRedirectDeflateBinding httpRedirectDeflateBinding() {
        return new HTTPRedirectDeflateBinding(parserPool());
    }

    @Bean
    public SAMLProcessorImpl processor() {
        final Collection<SAMLBinding> bindings = new ArrayList<>();
        bindings.add(httpRedirectDeflateBinding());
        bindings.add(httpPostBinding());
        return new SAMLProcessorImpl(bindings);
    }

    @Bean
    public HttpClient httpClient() throws IOException {
        return new HttpClient(multiThreadedHttpConnectionManager());
    }

    @Bean
    public MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager() {
        return new MultiThreadedHttpConnectionManager();
    }

    @Bean
    public static SAMLBootstrap sAMLBootstrap() {
        return new SAMLBootstrap();
    }

    @Bean
    public SAMLDefaultLogger samlLogger() {
        return new SAMLDefaultLogger();
    }

    @Bean
    public SAMLContextProviderImpl contextProvider() {
        return new SAMLContextProviderImpl();
    }

    // SAML 2.0 WebSSO Assertion Consumer
    @Bean
    public WebSSOProfileConsumer webSSOprofileConsumer() {
        return new WebSSOProfileConsumerImpl();
    }

    // SAML 2.0 Web SSO profile
    @Bean
    public WebSSOProfile webSSOprofile() {
        return new WebSSOProfileImpl();
    }

    // not used but autowired...
    // SAML 2.0 Holder-of-Key WebSSO Assertion Consumer
    @Bean
    public WebSSOProfileConsumerHoKImpl hokWebSSOprofileConsumer() {
        return new WebSSOProfileConsumerHoKImpl();
    }

    // not used but autowired...
    // SAML 2.0 Holder-of-Key Web SSO profile
    @Bean
    public WebSSOProfileConsumerHoKImpl hokWebSSOProfile() {
        return new WebSSOProfileConsumerHoKImpl();
    }

    @Bean
    public SingleLogoutProfile logoutProfile() {
        return new SingleLogoutProfileImpl();
    }

    @Bean
    @Qualifier("metadata")
    public CachingMetadataManager metadata() throws MetadataProviderException, AlertDatabaseConstraintException, AlertLDAPConfigurationException {
        return new CachingMetadataManager(Collections.emptyList());
    }

    @Bean
    public SAMLAuthenticationProvider samlAuthenticationProvider() {
        final SAMLAuthProvider samlAuthenticationProvider = new SAMLAuthProvider();
        samlAuthenticationProvider.setForcePrincipalAsString(false);
        return samlAuthenticationProvider;
    }
}
