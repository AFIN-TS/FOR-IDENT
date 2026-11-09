package de.hswt.fi.search.service.mass.search.model.properties;

import de.hswt.fi.beans.annotations.BeanColumn;
import de.hswt.fi.beans.annotations.BeanComponent;
import de.hswt.fi.search.service.mass.search.model.I18nKeys;
import de.hswt.fi.search.service.mass.search.model.JpaPreferences;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;
import java.util.Objects;

/**
 * The Class ECNumberStringProperty represents the EC Number string property in
 * the DB.
 *
 * @author Marco Luthardt
 */
@Embeddable
@BeanComponent
public class ECNumberStringProperty extends AbstractStringProperty {

	@Column(name = "ecNumber_value", length = JpaPreferences.LENGTH_EC)
	@BeanColumn(i18nId = I18nKeys.SI_MODEL_EC_NUMBER)
	protected String value;

	@Column(name = "ecNumber_source")
	@BeanColumn(i18nId = I18nKeys.SI_MODEL_SOURCE)
	protected String source;

	@Column(name = "ecNumber_editor")
	protected String editor;

	@Column(name = "ecNumber_additional", length = JpaPreferences.LENGTH_ADDITIONAL)
	@BeanColumn(i18nId = I18nKeys.SI_MODEL_ADDITIONAL)
	protected String additional;

	@Column(name = "ecNumber_lastModified")
	@Temporal(value = TemporalType.DATE)
	@BeanColumn(i18nId = I18nKeys.SI_MODEL_LAST_DATE, format = "TT:MM:YYYY")
	private Date lastModified;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ECNumberStringProperty)) return false;
		ECNumberStringProperty that = (ECNumberStringProperty) o;
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
