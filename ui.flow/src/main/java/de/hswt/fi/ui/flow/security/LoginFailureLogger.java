package de.hswt.fi.ui.flow.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/**
 * Logs failed logins with the client address (SP-17 security review), so that fail2ban can block an address after
 * repeated failures (deploy/fail2ban). Only the address is logged, not the entered user name. Behind the reverse proxy
 * the address is the client's ({@code server.forward-headers-strategy=native}).
 */
@Component
public class LoginFailureLogger {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoginFailureLogger.class);

	@EventListener
	public void loginFailed(AbstractAuthenticationFailureEvent event) {
		Object details = event.getAuthentication().getDetails();
		String address = details instanceof WebAuthenticationDetails web ? web.getRemoteAddress() : "unknown";
		LOGGER.warn("Login failed from {}", address);
	}
}
