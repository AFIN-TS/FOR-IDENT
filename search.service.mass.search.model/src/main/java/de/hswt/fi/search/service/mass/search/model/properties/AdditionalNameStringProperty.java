package de.hswt.fi.search.service.mass.search.model.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.hswt.fi.beans.annotations.BeanColumn;
import de.hswt.fi.beans.annotations.BeanComponent;
import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.I18nKeys;
import de.hswt.fi.search.service.mass.search.model.JpaPreferences;

import jakarta.persistence.*;
import java.util.Date;
import java.util.Objects;

/**
 * The Class AdditionalNameStringProperty represents the additional name string
 * property in the DB. It has an own table in the DB.
 *
 * @author Marco Luthardt
 */
@Entity
@Table(name = JpaPreferences.TABLE_NAME_ADDITIONAL_NAME)
@BeanComponent
public class AdditionalNameStringProperty extends AbstractStringProperty {

	@Id
	@GeneratedValue
	private Long id;

	@Column(length = JpaPreferences.LENGTH_NAME)
	@BeanColumn(i18nId = I18nKeys.SI_MODEL_NAME)
	private String value;

	@Column
	@BeanColumn(i18nId = I18nKeys.SI_MODEL_SOURCE)
	private String source;

	@Column
	private String editor;

	@Column(length = JpaPreferences.LENGTH_ADDITIONAL)
	private String additional;

	@Column(name = "lastModified")
	@Temporal(value = TemporalType.DATE)
	private Date lastModified;

	@JsonIgnore
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Entry substance;

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public void setSource(String source) {
		this.source = source;
	}

	@Override
	public String getEditor() {
		return editor;
	}

	@Override
	public void setEditor(String editor) {
		this.editor = editor;
	}

	@Override
	public String getAdditional() {
		return additional;
	}

	@Override
	public void setAdditional(String additional) {
		this.additional = additional;
	}

	@Override
	public Date getLastModified() {
		return lastModified;
	}

	@Override
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public void setSubstance(Entry substance) {
		this.substance = substance;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AdditionalNameStringProperty)) return false;
		AdditionalNameStringProperty that = (AdditionalNameStringProperty) o;
		return Objects.equals(value, that.value) &&
				Objects.equals(source, that.source) &&
				Objects.equals(editor, that.editor) &&
				Objects.equals(additional, that.additional) &&
				Objects.equals(lastModified, that.lastModified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, source, editor, additional, lastModified);
	}
}
