package de.hswt.fi.security.service.vaadin;

import de.hswt.fi.security.service.api.SecurityService;
import de.hswt.fi.security.service.api.SpringUserWrapper;
import de.hswt.fi.security.service.api.config.UserDatabaseConfiguration;
import de.hswt.fi.security.service.api.repositories.DatabaseRepository;
import de.hswt.fi.security.service.api.repositories.RegisteredUserRepository;
import de.hswt.fi.security.service.model.Database;
import de.hswt.fi.security.service.model.Group;
import de.hswt.fi.security.service.model.Groups;
import de.hswt.fi.security.service.model.RegisteredUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(transactionManager = UserDatabaseConfiguration.TRANSACTION_MANAGER)
public class DefaultSecurityService implements SecurityService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSecurityService.class);

	@PersistenceContext(unitName = UserDatabaseConfiguration.ENTITY_MANAGER)
	private EntityManager entityManager;

	private RegisteredUserRepository userRepository;

	private DatabaseRepository databaseRepository;

	private PasswordEncoder passwordEncoder;

	@Autowired
	public DefaultSecurityService(RegisteredUserRepository userRepository, DatabaseRepository databaseRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.databaseRepository = databaseRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void hashPassword(RegisteredUser user) {

		String hash = passwordEncoder.encode(user.getPassword());
		if (!passwordEncoder.matches(user.getPassword(), hash)) {
			throw new SecurityException("The password could not be created.");
		}
		user.setPassword(hash);

	}

	@Override
	public List<Database> findAllDatabases() {
		return databaseRepository.findAll();
	}

	@Override
	public RegisteredUser getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication.getPrincipal() instanceof SpringUserWrapper)) {
			return null;
		}
		return ((SpringUserWrapper) authentication.getPrincipal()).getUser();
	}

	@Override
	public boolean currentUserHasRole(String role) {
		boolean hasRole = false;
		SpringUserWrapper userDetails = getUserDetails();
		if (userDetails != null) {
			Collection<GrantedAuthority> authorities = userDetails.getAuthorities();
			if (isRolePresent(authorities, role)) {
				hasRole = true;
			}
		} else if (currentUserHasRoleAnonymous(role)) {
			hasRole = true;
		}
		return hasRole;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.hswt.riskident.security.api.SISecurityService#currentUserHasRole(java
	 * .util.List)
	 */
	@Override
	public boolean currentUserHasRole(List<String> roles) {
		boolean hasRole = false;
		SpringUserWrapper userDetails = getUserDetails();

		if (userDetails != null) {
			Collection<GrantedAuthority> authorities = userDetails.getAuthorities();
			for (String role : roles) {
				if (isRolePresent(authorities, role)) {
					hasRole = true;
				}
			}
		} else if (currentUserHasRoleAnonymous(roles)) {
			hasRole = true;
		}
		return hasRole;
	}

	@Override
	public boolean requestUserAccount(RegisteredUser user) {
		return createUser(user) != null;
	}

	@Override
	public boolean deleteUser(RegisteredUser user) {
		LOGGER.debug("entering method deleteUser");
		try {
			checkUser(user);
		} catch (IllegalArgumentException e) {
			LOGGER.error("leave method deleteUser, user is invalid", e);
			return false;
		}

		RegisteredUser u = entityManager.find(RegisteredUser.class, user.getId());
		entityManager.remove(u);

		boolean res = true;
		if (userExistsById(user.getId())) {
			res = false;
		}

		entityManager.close();
		LOGGER.debug("leave method deleteUser, return {}", res);
		return res;
	}

	@Override
	public boolean resetPasswordTo(String mail, String newPassword) {
		boolean success = false;
		LOGGER.debug("enter resetPasswordTo, for user with mail {}", mail);
		RegisteredUser user = getUserByMail(mail);
		if (user != null) {
			user.setPassword(newPassword);
			hashPassword(user);
			success = updateUser(user) != null;
		}
		LOGGER.debug("leave resetPasswordTo, return {}", success);
		return success;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.hswt.riskident.security.api.SISecurityService#findAllGroups()
	 */
	@Override
	public List<Group> findAllGroups() {
		return findAllGroupsAsList();
	}

	/**
	 * Find all groups as list.
	 *
	 * @return the list<? extends si group>
	 */
	private List<Group> findAllGroupsAsList() {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Group> cq = cb.createQuery(Group.class);
		Root<Group> entry = cq.from(Group.class);
		cq.select(entry);
		cq.orderBy(cb.asc(entry.get("name")));
		TypedQuery<Group> query = entityManager.createQuery(cq);
		List<Group> results = query.getResultList();

		entityManager.close();
		return results;
	}

	@Override
	public List<RegisteredUser> findAllUsers() {
		return (List<RegisteredUser>) userRepository.findAll();
	}

	@Override
	public RegisteredUser updateUser(RegisteredUser user) {
		LOGGER.debug("enter updateUser, user {}", user);
		checkUser(user);
		LOGGER.debug("check user successful");

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		entityManager.merge(user);

		LOGGER.debug("updated user: {}", user.getUsername());

		CriteriaQuery<RegisteredUser> cq = cb.createQuery(RegisteredUser.class);
		Root<RegisteredUser> entry = cq.from(RegisteredUser.class);
		cq.select(entry);
		cq.where(cb.equal(entry.<String> get("id"), user.getId()));
		TypedQuery<RegisteredUser> query = entityManager.createQuery(cq);
		RegisteredUser updated = null;
		LOGGER.trace("try searching updated user in db");
		try {
			updated = query.getSingleResult();
			LOGGER.trace("found updated user in db: {}", updated.getUsername());
		} catch (NoResultException e) {
			LOGGER.error(e.getMessage(), e);
		}

		if (!user.equals(updated)) {
			LOGGER.debug("update not successful");
			updated = null;
		} else {
			LOGGER.debug("update successful");
		}

		entityManager.close();

		LOGGER.debug("leaving updateUser, return {}", updated);

		return updated;
	}

	@Override
	public boolean userExists(String username) {
		return username != null && getUser("username", username.toLowerCase()) != null;
	}

	@Override
	public boolean mailExists(String mail) {
		return mail != null && !mail.isEmpty() && getUser("mail", mail.toLowerCase()) != null;
	}

	private RegisteredUser getUserByMail(String mail) {
		if (mail == null || mail.isEmpty()) {
			return null;
		}

		return getUser("mail", mail.toLowerCase());
	}

	private RegisteredUser getUser(String parameter, String targetValue) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<RegisteredUser> cq = cb.createQuery(RegisteredUser.class);
		Root<RegisteredUser> entry = cq.from(RegisteredUser.class);
		cq.select(entry);
		cq.where(cb.equal(entry.<String> get(parameter), targetValue));
		TypedQuery<RegisteredUser> query = entityManager.createQuery(cq);
		RegisteredUser result = null;
		try {
			result = query.getSingleResult();
		} catch (NoResultException e) {
			LOGGER.error("Aa error occured {}", e.getMessage());
		}

		entityManager.close();
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.hswt.riskident.security.api.SISecurityService#userExistsById(java.
	 * lang.String)
	 */
	@Override
	public boolean userExistsById(Long id) {
		return id != null && entityManager.find(RegisteredUser.class, id) != null;

	}

	@Override
	public RegisteredUser createUser(RegisteredUser user) {
		LOGGER.debug("entering method createUser");

		validateUserObject(user);

		if (userExists(user.getUsername())) {
			LOGGER.debug("desired username {} already exists in method createUserInternal",
					user.getUsername());
			throw new IllegalArgumentException("A user with the given username already exists.");
		}

		hashPassword(user);
		addUserToGroup(user, Groups.GROUP_USER);

		entityManager.persist(user);
        RegisteredUser newUser = entityManager.find(RegisteredUser.class, user.getId());
        entityManager.close();

        if (newUser == null) {
            LOGGER.debug("create user {} in method createUserInternal failed", user.getUsername());
        } else {
            findAllDatabases().stream().filter(Database::isPublicAccessible).forEach(newUser::addAccessbileDatabase);
            updateUser(newUser);
        }

		return newUser;
	}

	@Override
    public Group createGroup(Group group) {
	    if (groupExists(group)) {
	        LOGGER.error("Group: {} already exists",group.getName());
        }
	    entityManager.persist(group);
		LOGGER.error("Created Group: {}",group.getName());
	    return group;
    }

    private boolean groupExists(Group group) {
        return findAllGroups().stream().anyMatch(persistedGroups -> persistedGroups.equals(group));
    }

    private void addUserToGroup(RegisteredUser user, Groups group) {

		user.setEnabled(false);

        Optional<Group> foundGroup = findGroup(group);

		if (!foundGroup.isPresent()) {
			throw new InvalidParameterException(Groups.GROUP_USER.toString() + " not found");
		}
		user.addGroup(foundGroup.get());
	}

	private Optional<Group> findGroup(Groups groups) {
        return findAllGroups().stream()
                .filter(g -> g.getName().equals(groups.toString()))
                .findFirst();
    }

	private SpringUserWrapper getUserDetails() {
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			return null;
		}
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		SpringUserWrapper userDetails = null;
		if (principal instanceof SpringUserWrapper) {
			userDetails = (SpringUserWrapper) principal;
		}
		return userDetails;
	}

	/**
	 * Current user has role anonymous.
	 *
	 * @param role
	 *            the role
	 * @return true, if successful
	 */
	private boolean currentUserHasRoleAnonymous(String role) {
		boolean anonymous = false;
		if (role.equals(ROLE_ANONYMOUS) && isCurrentUserAnonymous()) {
			anonymous = true;
		}
		return anonymous;
	}

	/**
	 * Current user has role anonymous.
	 *
	 * @param roles
	 *            the roles
	 * @return true, if successful
	 */
	private boolean currentUserHasRoleAnonymous(List<String> roles) {
		boolean anonymous = false;
		if (roles.contains(ROLE_ANONYMOUS) && isCurrentUserAnonymous()) {
			anonymous = true;
		}
		return anonymous;
	}

	/**
	 * Check user.
	 *
	 * @param user
	 *            the user
	 */
	private void checkUser(RegisteredUser user) {

		validateUserObject(user);

		if (user.getId() != null && !userExistsById(user.getId())) {
			throw new IllegalArgumentException("The user is not in database.");
		}
	}

	/**
	 * Checks if is current user anonymous.
	 *
	 * @return true, if is current user anonymous
	 */
	private boolean isCurrentUserAnonymous() {
		boolean anonymous = false;
		if (SecurityContextHolder.getContext()
				.getAuthentication() instanceof AnonymousAuthenticationToken) {
			anonymous = true;
		}
		return anonymous;
	}

	/**
	 * Check if a role is present in the authorities of current user.
	 *
	 * @param authorities
	 *            all authorities assigned to current user
	 * @param role
	 *            required authority
	 * @return true if role is present in list of authorities assigned to
	 *         current user, false otherwise
	 */
	private boolean isRolePresent(Collection<GrantedAuthority> authorities, String role) {
		boolean isRolePresent = false;
		for (GrantedAuthority grantedAuthority : authorities) {
			isRolePresent = grantedAuthority.getAuthority().equals(role);
			if (isRolePresent) {
				break;
			}
		}
		return isRolePresent;
	}

	/**
	 * Check if a the RegisteredUser object has valid fields.
	 *
	 * @param user
	 * 			the RegisteredUser object
	 *
	 * @throws IllegalArgumentException if the user, its username ot password is null or empty
	 *
	 * */
	private void validateUserObject(RegisteredUser user) {
		if (user == null) {
			throw new IllegalArgumentException(
					"The user must not be null.");
		}

		if (user.getPassword() == null || user.getPassword().isEmpty()
				|| user.getUsername() == null || user.getUsername().isEmpty()) {
			LOGGER.debug("invalid parameter in method createUserInternal");
			throw new IllegalArgumentException(
					"The user must have a username and password.");
		}
	}
}
