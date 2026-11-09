package de.hswt.fi.security.service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "tbl_databases")
public class Database {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	private boolean publicAccessible;

	public Database() {
	}

	public Database(String name, boolean publicAccessible) {
		this.name = name;
		this.publicAccessible = publicAccessible;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isPublicAccessible() {
		return publicAccessible;
	}

	public void setPublicAccessible(boolean publicAccessible) {
		this.publicAccessible = publicAccessible;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Database database = (Database) o;
		return Objects.equals(name, database.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}
