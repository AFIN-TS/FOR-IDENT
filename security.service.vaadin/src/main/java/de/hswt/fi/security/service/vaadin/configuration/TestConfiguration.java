package de.hswt.fi.security.service.vaadin.configuration;

import de.hswt.fi.common.spring.Profiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * This class configures the spring security behaviour for
 * integration tests, enabling url path to h2 in memory database console.
 *
 * @author August Gilg
 */

@Profile(Profiles.TEST)
@Configuration
public class TestConfiguration {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        http.csrf(AbstractHttpConfigurer::disable);
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));
        return http.build();
    }

    @Bean
    public UserDetailsService testUserDetailsService() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        return new InMemoryUserDetailsManager(User.withUsername("admin")
                .password(encoder.encode("123456"))
                .roles("ADMIN", "USER")
                .build());
    }
}
