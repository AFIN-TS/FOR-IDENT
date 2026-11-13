package de.hswt.fi.application;

import de.hswt.fi.common.spring.Profiles;
import de.hswt.fi.security.service.api.SecurityService;
import de.hswt.fi.security.service.model.Group;
import de.hswt.fi.security.service.model.Groups;
import de.hswt.fi.security.service.model.RegisteredUser;
import de.hswt.fi.security.service.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartupListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    static final String ADMIN_PASSWORD_PROPERTY = "FI_ADMIN_PASSWORD";

    static final String ADMIN_MAIL_PROPERTY = "FI_ADMIN_MAIL";

    private final SecurityService securityService;

    @Autowired
    public ApplicationStartupListener(SecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        Environment environment = contextRefreshedEvent.getApplicationContext().getEnvironment();
        if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of(Profiles.INITIALIZATION_LC, Profiles.INITIALIZATION_GC))) {
            LOGGER.debug("Running application in initialization mode!");
            initAdminAccount(securityService, getAdminPassword(environment), getAdminMail(environment));
        }
    }

    /**
     * The initial admin password from FI_ADMIN_PASSWORD. Required: no password is generated, so none ends up in the
     * log (SP-20, review finding S8).
     */
    static String getAdminPassword(Environment environment) {
        String password = environment.getProperty(ADMIN_PASSWORD_PROPERTY);
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(ADMIN_PASSWORD_PROPERTY
                    + " must be set for the initialization profile (password of the initial user admin)");
        }
        return password;
    }

    /**
     * The mail address of the initial admin from FI_ADMIN_MAIL. Required: a fixed default address would receive the
     * password reset links of the admin (SP-20, review finding S4).
     */
    static String getAdminMail(Environment environment) {
        String mail = environment.getProperty(ADMIN_MAIL_PROPERTY);
        if (mail == null || mail.isBlank() || !mail.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalStateException(ADMIN_MAIL_PROPERTY
                    + " must be set to a valid address for the initialization profile (mail of the initial user admin)");
        }
        return mail.trim();
    }

    private void initAdminAccount(SecurityService securityService, String adminPassword, String adminMail) {

        LOGGER.debug("Creating groups and roles ...");

        Group groupAdmin = new Group();
        groupAdmin.setName(Groups.GROUP_ADMIN.name());
        groupAdmin.addAuthority(Role.ROLE_ADMIN);

        Group groupUser = new Group();
        groupUser.setName(Groups.GROUP_USER.name());
        groupUser.addAuthority(Role.ROLE_USER);

        Group groupScientist = new Group();
        groupScientist.setName(Groups.GROUP_SCIENTIST.name());
        groupScientist.addAuthority(Role.ROLE_SCIENTIST);

        securityService.createGroup(groupAdmin);
        securityService.createGroup(groupUser);
        securityService.createGroup(groupScientist);

        LOGGER.debug("Creating admin user ...");

		RegisteredUser adminUser = new RegisteredUser();
		adminUser.setUsername("admin");
		adminUser.setPassword(adminPassword);
		adminUser.setFirstname("Administrator");
		adminUser.setLastname("Administrator");
		adminUser.setMail(adminMail);
		adminUser.setOrganisation("");
        adminUser.addGroup(groupAdmin);

        // createUser and updateUser are secured (@PreAuthorize). During initialization nobody is
        // logged in, so the admin account is created with a temporary system authentication.
        // An anonymous token is used, so the security expressions only check hasRole('ROLE_ADMIN').
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new AnonymousAuthenticationToken("initialization", "initialization",
                AuthorityUtils.createAuthorityList(Role.ROLE_ADMIN.name())));
        SecurityContextHolder.setContext(securityContext);
        try {
            securityService.createUser(adminUser);
            adminUser.setEnabled(true);
            securityService.updateUser(adminUser);
        } finally {
            SecurityContextHolder.clearContext();
        }

		LOGGER.debug("Everything done!");
    }
}