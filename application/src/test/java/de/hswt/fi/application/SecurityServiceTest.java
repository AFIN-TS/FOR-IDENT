package de.hswt.fi.application;

import de.hswt.fi.security.service.api.SecurityService;
import de.hswt.fi.security.service.model.RegisteredUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityServiceTest extends AbstractTest {

    @Autowired
    private SecurityService securityService;

    @Override
    public void setup() {

    }

    @Override
    public void cleanup() {
    }

    @Test
    @WithMockUser(roles="ADMIN")
    public void createAndDeleteUserWithUserDetails() {
        RegisteredUser testUser = getTestUser();
        int numberOfUser = securityService.findAllUsers().size();
        testUser = securityService.createUser(testUser);
        assertEquals(numberOfUser + 1, securityService.findAllUsers().size());
        assertNotNull(testUser.getId());
        assertTrue(securityService.deleteUser(testUser));
        assertEquals(numberOfUser, securityService.findAllUsers().size());
    }

    private RegisteredUser getTestUser() {
        RegisteredUser testUser = new RegisteredUser();
        testUser.setUsername(UUID.randomUUID().toString());
        testUser.setMail(UUID.randomUUID().toString() + "@test.de");
        testUser.setPassword("123456");
        return testUser;
    }
}
