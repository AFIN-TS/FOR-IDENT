package de.hswt.fi.ui.flow.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import de.hswt.fi.common.spring.Profiles;
import de.hswt.fi.ui.flow.views.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Access rules of the web application (SP-11).
 * <ul>
 * <li>REST API {@code /api/**}: public and read-only as before, no session login.</li>
 * <li>Vaadin Flow UI: login view, views open to everybody ({@code @AnonymousAllowed}) or only to logged in users or
 * roles ({@code @PermitAll}, {@code @RolesAllowed}). CSRF protection is on (the Vaadin 8 UI had it switched off).</li>
 * <li>Static pages of the home and help views (html folder, {@code spring.web.resources.static-locations}) and the
 * images of the UI are public.</li>
 * </ul>
 */
@Profile({Profiles.LC, Profiles.DEVELOPMENT_LC, Profiles.GC, Profiles.DEVELOPMENT_GC})
@Configuration
public class UiSecurityConfiguration {

	@Bean
	@Order(1)
	public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/api/**");
		http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
		// Read-only GET requests without login: no session state that CSRF tokens would protect
		http.csrf(AbstractHttpConfigurer::disable);
		http.httpBasic(AbstractHttpConfigurer::disable);
		http.formLogin(AbstractHttpConfigurer::disable);
		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain uiSecurityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(requests -> requests
				.requestMatchers("/*.html", "/css/**", "/logos/**", "/download/**", "/img/**", "/styles/**",
						// Error page: a missing static file gives 404, not 401
						"/error",
						// Health checks for monitoring and the reverse proxy, without details (SP-16)
						"/actuator/health", "/actuator/health/**")
				.permitAll());
		// The home view shows the static home page in an iframe of the same origin
		http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
		http.with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin.loginView(LoginView.class, "/"));
		return http.build();
	}
}
