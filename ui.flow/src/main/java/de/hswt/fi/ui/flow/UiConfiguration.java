package de.hswt.fi.ui.flow;

import com.vaadin.flow.spring.annotation.EnableVaadin;
import org.springframework.context.annotation.Configuration;

/**
 * Vaadin looks for routes, layouts and the app shell in the package of the Spring Boot application class
 * (de.hswt.fi.application) unless the packages are given here.
 */
@Configuration
@EnableVaadin("de.hswt.fi.ui.flow")
public class UiConfiguration {
}
