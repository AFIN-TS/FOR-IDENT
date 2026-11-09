package de.hswt.fi.security.service.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Table(name = "tbl_user_groups")
public class Group {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	@ElementCollection(targetClass = Role.class)
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private List<Role> authorities = new ArrayList<>();

	@ManyToMany(mappedBy = "groups")
	private List<RegisteredUser> user = new ArrayList<>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.hswt.riskident.security.api.SIGroup#getId()
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Sets the id.
	 *
	 * @param id
	 *            the new id
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.hswt.riskident.security.api.SIGroup#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name.
	 *
	 * @param name
	 *            the new name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.hswt.riskident.security.api.SIGroup#getAuthorities()
	 */
	public List<String> getAuthorities() {
		return authorities.stream().map(Enum::name).collect(Collectors.toList());
	}

	/**
	 * Adds the user.
	 *
	 * @param user
	 *            the user
	 */
	public void addUser(RegisteredUser user) {
		if (user == null || this.user.contains(user)) {
			return;
		}
		this.user.add(user);
		user.addGroup(this);
	}

	public void addAuthority(Role role) {
		authorities.add(role);
	}

	/**
	 * Removes the user.
	 *
	 * @param user
	 *            the user
	 */
	public void removeUser(RegisteredUser user) {
		if (user == null || !this.user.contains(user)) {
			return;
		}
		this.user.remove(user);
		user.removeGroup(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Group group = (Group) o;
		return Objects.equals(name, group.name) &&
				Objects.equals(authorities, group.authorities);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, authorities);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Group [id=");
		builder.append(id);
		builder.append(", name=");
		builder.append(name);
		builder.append(", authorities=");
		builder.append(authorities);
		builder.append("]");
		return builder.toString();
	}
}
