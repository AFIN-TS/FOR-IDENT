package de.hswt.fi.userproperties.service.model;

import de.hswt.fi.security.service.model.RegisteredUser;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.AccessType.Type;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Properties;


@Entity
@AccessType(Type.FIELD)
@Table(name = "userproperties")
public class UserProperties implements Serializable {

	private static final long serialVersionUID = 8762860994080474316L;

	@Id
	@GeneratedValue
	@Column
	private Long userID;

	@OneToOne(cascade = CascadeType.MERGE)
	@PrimaryKeyJoinColumn
	private RegisteredUser user;

	@Lob
	private Properties properties;

	protected UserProperties() {
		// No parameter constructor needed for entity
	}

	public UserProperties(Long userID, Properties properties) {
		this.userID = userID;
		this.properties = properties;
	}

	public Long getId() {
		return userID;
	}

	public RegisteredUser getUser() {
		return user;
	}

	public Properties getProperties() {
		return properties;
	}

}
