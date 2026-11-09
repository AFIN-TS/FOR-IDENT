package de.hswt.fi.security.service.model;

import org.eclipse.persistence.annotations.BatchFetch;
import org.eclipse.persistence.annotations.BatchFetchType;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "tbl_user")
public class RegisteredUser {

	@Id
	@GeneratedValue
	private Long id;

	private String username;

	private String password;

	private String firstname;

	private String lastname;

	private String organisation;

	private String mail;

	private boolean enabled = false;

	@ManyToMany
	@BatchFetch(BatchFetchType.JOIN)
	private Collection<Group> groups = new ArrayList<>();

	@ManyToMany
	@BatchFetch(BatchFetchType.JOIN)
	private List<Database> accessibleDatabases = new ArrayList<>();

	public RegisteredUser() {
	}

	public RegisteredUser(String username, String password) {
		if (password == null || password.isEmpty() || username == null || username.isEmpty()) {
			throw new IllegalArgumentException(
					"The username and password must not be null or empty.");
		}
		this.password = password;
		this.username = username.toLowerCase();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		if (username == null || username.isEmpty()) {
			throw new IllegalArgumentException("The username must not be null or empty.");
		}
		this.username = username.toLowerCase();
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		if (password == null || password.isEmpty()) {
			throw new IllegalArgumentException("The password must not be null or empty.");
		}
		this.password = password;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getOrganisation() {
		return organisation;
	}

	public void setOrganisation(String organisation) {
		this.organisation = organisation;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		if (mail != null) {
			this.mail = mail.toLowerCase();
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Collection<Group> getGroups() {
		return groups;
	}

	public void addGroup(Group group) {
		if (groups.contains(group)) {
			return;
		}
		groups.add(group);
		group.addUser(this);
	}

	public void removeGroup(Group group) {
		if (group == null || !groups.contains(group)) {
			return;
		}
		groups.remove(group);
		group.removeUser(this);
	}

	public List<Database> getAccessibleDatabases() {
		return accessibleDatabases;
	}

	public void addAccessbileDatabase(Database database) {
		if (!accessibleDatabases.contains(database)) {
			accessibleDatabases.add(database);
		}
	}

	public void removeAllAccessibleDatabases() {
		accessibleDatabases.clear();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RegisteredUser that = (RegisteredUser) o;
		return Objects.equals(mail, that.mail);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mail);
	}

	@Override
	public String toString() {
		return "RegisteredUser{" +
				"id='" + id + '\'' +
				", username='" + username + '\'' +
				", firstname='" + firstname + '\'' +
				", lastname='" + lastname + '\'' +
				", organisation='" + organisation + '\'' +
				", mail='" + mail + '\'' +
				", enabled=" + enabled +
				", groups=" + groups +
				'}';
	}
}
