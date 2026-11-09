package de.hswt.fi.mail.service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;

public class Mail implements Serializable {

	private static final long serialVersionUID = -1167922604050263403L;

	@NotBlank(message = "{mail.name.notBlank}")
	@Size(min = 1, max = 60, message = "{mail.name.size}")
	private String senderName;

	@NotBlank(message = "{mail.address.notBlank}")
	@Size(min = 1, max = 100, message = "{mail.address.size}")
	@Pattern(regexp = "[\\w|-]+@\\w[\\w|-]*\\.[a-z]{2,3}", message = "{mail.address.pattern}")
	private String senderMailAddress;

	@NotBlank(message = "{mail.subject.notBlank}")
	@Size(min = 1, max = 100, message = "{mail.subject.size}")
	private String subject;

	@NotBlank(message = "{mail.message.notBlank}")
	@Size(min = 1, max = 4000, message = "{mail.message.size}")
	private String message;

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public String getSenderMailAddress() {
		return senderMailAddress;
	}

	public void setSenderMailAddress(String senderMailAddress) {
		this.senderMailAddress = senderMailAddress;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Mail mail = (Mail) o;
		return Objects.equals(senderName, mail.senderName) &&
				Objects.equals(senderMailAddress, mail.senderMailAddress) &&
				Objects.equals(subject, mail.subject) &&
				Objects.equals(message, mail.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(senderName, senderMailAddress, subject, message);
	}

	@Override
	public String toString() {
		return "Mail [senderName=" +
				senderName +
				", senderMailAddress=" +
				senderMailAddress +
				", subject=" +
				subject +
				", message=" +
				message +
				"]";
	}

}
