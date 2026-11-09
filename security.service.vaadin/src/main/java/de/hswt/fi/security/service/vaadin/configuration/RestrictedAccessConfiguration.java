package de.hswt.fi.security.service.vaadin.configuration;

import de.hswt.fi.common.spring.Profiles;
import de.hswt.fi.security.service.vaadin.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

/**
 * This class configures the spring security behaviour: authentication against the user database and method
 * security of the services ({@code @Secured}, {@code @PreAuthorize}).
 * <p>
 * The filter chains (REST API and the access rules of the views) are defined by the UI module (ui.flow,
 * UiSecurityConfiguration) since SP-11.
 *
 * @author Tobias Placht
 */

@Profile({Profiles.LC, Profiles.DEVELOPMENT_LC, Profiles.GC, Profiles.DEVELOPMENT_GC})
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, proxyTargetClass = true)
public class RestrictedAccessConfiguration {

    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    public RestrictedAccessConfiguration(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * Protection against session fixation by changing the session ID on login (Servlet 3.1): the session
     * and its attributes are kept. {@code SessionFixationProtectionStrategy} replaced the session instead;
     * ending the old session deleted the directory of the uploaded files that the new session still uses
     * (see CustomHttpSessionListener).
     */
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }
}
