package de.hswt.fi.security.service.vaadin.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Login must keep the session: the directory of the uploaded files is a session attribute and is deleted
 * when a session ends. Replacing the session on login deleted the directory the new session still used.
 */
public class SessionAuthenticationStrategyTest {

	@Test
	public void loginKeepsSessionAndChangesSessionId() {
		SessionAuthenticationStrategy strategy = new RestrictedAccessConfiguration(null).sessionAuthenticationStrategy();
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("temp-directory", "session-directory");
		request.setSession(session);
		// As sent by the browser (the Spring 7 mock request no longer assumes a valid requested session ID)
		request.setRequestedSessionId(session.getId());
		request.setRequestedSessionIdValid(true);
		String idBeforeLogin = session.getId();

		strategy.onAuthentication(new TestingAuthenticationToken("user", "password"), request, new MockHttpServletResponse());

		assertSame(session, request.getSession(false));
		assertFalse(session.isInvalid());
		assertNotEquals(idBeforeLogin, session.getId());
		assertEquals("session-directory", session.getAttribute("temp-directory"));
	}
}
