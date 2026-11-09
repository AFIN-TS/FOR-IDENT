package de.hswt.fi.search.service.mass.search.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = JpaPreferences.TABLE_NAME_SOURCE_LISTS)
public class SourceList implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long id;

	@Column
	private String name;

	@Column
	private String description;

	@JsonIgnore
	@ManyToMany(mappedBy = "sourceLists")
	private Set<Entry> substances;

	public SourceList() {
	}

	public SourceList(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SourceList that = (SourceList) o;
		return Objects.equals(name, that.name) &&
				Objects.equals(description, that.description);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, description);
	}

	@Override
	public String toString() {
		return "SourceList [name=" + name + "]";
	}
}
