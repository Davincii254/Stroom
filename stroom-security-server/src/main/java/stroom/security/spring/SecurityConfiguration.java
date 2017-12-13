/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.security.spring;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.filter.authc.AnonymousFilter;
import org.apache.shiro.web.servlet.AbstractShiroFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import stroom.apiclients.AuthenticationServiceClients;
import stroom.security.server.JWTAuthenticationFilter;
import stroom.security.server.JWTService;
import stroom.security.server.NonceManager;
import stroom.util.config.StroomProperties;
import stroom.util.spring.StroomScope;

import javax.annotation.Resource;
import javax.servlet.Filter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

/**
 * The authentication providers are configured manually because the method
 * signature of the
 *
 * @Override configure() method doesn't allow us to pass the @Components we need
 * to.
 */

/**
 * Exclude other configurations that might be found accidentally during a
 * component scan as configurations should be specified explicitly.
 */
@Configuration
@ComponentScan(basePackages = {"stroom.security.server"}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION, value = Configuration.class),})
public class SecurityConfiguration {
    public static final String PROD_SECURITY = "PROD_SECURITY";
    public static final String MOCK_SECURITY = "MOCK_SECURITY";
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Resource
    private SecurityManager securityManager;

    @Bean(name = "jwtFilter")
    public JWTAuthenticationFilter jwtAuthenticationFilter(
            @Value("#{propertyConfigurer.getProperty('stroom.auth.service.url')}")
            final String authenticationServiceUrl,
            @Value("#{propertyConfigurer.getProperty('stroom.advertisedUrl')}")
            final String advertisedStroomUrl,
            @Value("#{propertyConfigurer.getProperty('stroom.auth.jwt.issuer')}")
            final String jwtIssuer,
            JWTService jwtService,
            NonceManager nonceManager,
            AuthenticationServiceClients authenticationServiceClients) {
        return new JWTAuthenticationFilter(
                authenticationServiceUrl, advertisedStroomUrl, jwtIssuer,
                jwtService, nonceManager, authenticationServiceClients);
    }

    @Bean(name = "shiroFilter")
    public AbstractShiroFilter shiroFilter(final JWTAuthenticationFilter jwtAuthenticationFilter,
       @Value("#{propertyConfigurer.getProperty('stroom.ui.login.url')}") final String loginUrl) throws Exception {
        final ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
        shiroFilter.setSecurityManager(securityManager);
        shiroFilter.setLoginUrl(loginUrl);
        shiroFilter.setSuccessUrl("/stroom.jsp");

        Map<String, Filter> filters = shiroFilter.getFilters();
        filters.put("jwtFilter", jwtAuthenticationFilter);
        filters.put("anonymousFilter", new AnonymousFilter());

        shiroFilter.getFilterChainDefinitionMap().put("/**/secure/**", "authc, roles[USER]");
        // Allow anonymous access to the getToken resource.
        shiroFilter.getFilterChainDefinitionMap().put("/api/authentication/v*/getToken", "anonymousFilter");
        shiroFilter.getFilterChainDefinitionMap().put("/**", "jwtFilter");
        shiroFilter.getFilterChainDefinitionMap().put("/api/**", "jwtFilter");
        return (AbstractShiroFilter) shiroFilter.getObject();
    }

    @Bean(name = "mailSender")
    @Scope(StroomScope.PROTOTYPE)
    public MailSender mailSender() {
        final String host = StroomProperties.getProperty("stroom.mail.host");
        final int port = StroomProperties.getIntProperty("stroom.mail.port", 587);
        final String protocol = StroomProperties.getProperty("stroom.mail.protocol", "smtp");
        final String userName = StroomProperties.getProperty("stroom.mail.userName");
        final String password = StroomProperties.getProperty("stroom.mail.password");

        String propertiesFile = StroomProperties.getProperty("stroom.mail.propertiesFile");

        final JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(host);
        javaMailSender.setPort(port);
        javaMailSender.setProtocol(protocol);

        if (!StringUtils.isEmpty(userName)) {
            javaMailSender.setUsername(userName);
        }
        if (!StringUtils.isEmpty(password)) {
            javaMailSender.setPassword(password);
        }

        if (!StringUtils.isEmpty(propertiesFile)) {
            propertiesFile = propertiesFile.replaceAll("~", System.getProperty("user.home"));

            final Path file = Paths.get(propertiesFile);
            if (Files.isRegularFile(file)) {
                try (final InputStream is = Files.newInputStream(file)) {
                    final Properties properties = new Properties();
                    properties.load(is);
                    javaMailSender.setJavaMailProperties(properties);
                } catch (final IOException e) {
                    LOGGER.warn("Unable to load mail properties '" + propertiesFile + "'");
                }
            } else {
                LOGGER.warn("Mail properties not found at '" + propertiesFile + "'");
            }
        }

        return javaMailSender;
    }

    @Bean
    @Scope(StroomScope.PROTOTYPE)
    public SimpleMailMessage resetPasswordTemplate() {
        final SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setSubject("Stroom - Password Reset");
        simpleMailMessage.setText("Dear ${username},\n\nYour Stroom password for host '${hostname}' has been reset.\n\nYour new password is '${password}'.\n\nThank you.");
        return simpleMailMessage;
    }

//    @Bean
//    public AuthenticationResource authenticationResource(final Environment environment, final AuthenticationService authenticationService, final JWTService jwtService) {
//        final AuthenticationResource authenticationResource = new AuthenticationResource(authenticationService, jwtService);
//
//        // Add resource.
//        environment.jersey().register(authenticationResource);
//
//        return authenticationResource;
//    }
//
//    @Bean
//    public AuthorisationResource authorisationResource(final Environment environment, final AuthorisationService authorisationService) {
//        final AuthorisationResource authorisationResource = new AuthorisationResource(authorisationService);
//
//        // Add resource.
//        environment.jersey().register(authorisationResource);
//
//        return authorisationResource;
//    }
}
