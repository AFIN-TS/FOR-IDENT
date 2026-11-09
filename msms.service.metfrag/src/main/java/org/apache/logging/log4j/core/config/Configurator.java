package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Level;

/**
 * Replacement for the Log4j 2 {@code Configurator} of log4j-core, for MetFragLib only.
 *
 * <p>MetFragLib calls {@code Configurator.setLevel(loggerName, level)} to set the level of its own
 * loggers. FOR-IDENT logs with Logback (Spring Boot); Log4j 2 API calls are routed to it by
 * log4j-to-slf4j, and log4j-core is not on the classpath (it would fail with a ClassCastException
 * with that routing). The log levels are set in the logging configuration of FOR-IDENT instead.
 */
public final class Configurator {

	private Configurator() {
	}

	public static void setLevel(String loggerName, Level level) {
		// Intentionally empty, see class comment
	}
}
