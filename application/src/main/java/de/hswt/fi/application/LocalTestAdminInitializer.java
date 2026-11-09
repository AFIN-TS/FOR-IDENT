package de.hswt.fi.application;

import de.hswt.fi.security.service.api.SecurityService;
import de.hswt.fi.security.service.api.repositories.RegisteredUserRepository;
import de.hswt.fi.security.service.model.Group;
import de.hswt.fi.security.service.model.Groups;
import de.hswt.fi.security.service.model.RegisteredUser;
import de.hswt.fi.security.service.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Local tests only (profile {@code local-test-admin}, set by {@code dev/run-local.ps1 -TestAdmin}): creates or updates
 * the administrator account {@value #USERNAME} with the password from {@code FI_TEST_ADMIN_PASSWORD}, so the UI can be
 * tested without the real admin password. Never active in production.
 */
@Component
@Profile("local-test-admin")
public class LocalTestAdminInitializer {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalTestAdminInitializer.class);

	static final String USERNAME = "test-admin@example.test";

	private final SecurityService securityService;

	private final RegisteredUserRepository userRepository;

	private final Environment environment;

	public LocalTestAdminInitializer(SecurityService securityService, RegisteredUserRepository userRepository,
									 Environment environment) {
		this.securityService = securityService;
		this.userRepository = userRepository;
		this.environment = environment;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void createTestAdmin() {
		String password = environment.getProperty("FI_TEST_ADMIN_PASSWORD");
		if (password == null || password.length() < 12) {
			LOGGER.warn("Profile local-test-admin without FI_TEST_ADMIN_PASSWORD (at least 12 characters): no test admin");
			return;
		}
		// createUser and updateUser are secured; nobody is logged in at startup (see ApplicationStartupListener)
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(new AnonymousAuthenticationToken("local-test-admin", "local-test-admin",
				AuthorityUtils.createAuthorityList(Role.ROLE_ADMIN.name())));
		SecurityContextHolder.setContext(context);
		try {
			RegisteredUser user = userRepository.findByUsername(USERNAME).orElse(null);
			if (user == null) {
				user = new RegisteredUser(USERNAME, password);
				user.setMail(USERNAME);
				user.setFirstname("Local");
				user.setLastname("Test admin");
				user.setOrganisation("Local tests");
				user = securityService.createUser(user);
			} else {
				user.setPassword(password);
				securityService.hashPassword(user);
			}
			user.setEnabled(true);
			RegisteredUser admin = user;
			securityService.findAllGroups().stream()
					.filter(group -> group.getName().equals(Groups.GROUP_ADMIN.name()))
					.filter(group -> admin.getGroups().stream().map(Group::getName).noneMatch(group.getName()::equals))
					.forEach(admin::addGroup);
			securityService.findAllDatabases().forEach(database -> {
				if (!admin.getAccessibleDatabases().contains(database)) {
					admin.addAccessbileDatabase(database);
				}
			});
			securityService.updateUser(admin);
			LOGGER.warn("Local test admin {} is active (profile local-test-admin)", USERNAME);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}
}
