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
 * The Class MassNumberProperty represents the accurate mass number property in
 * the DB.
 *
 * @author Marco Luthardt
 */
@Embeddable
@BeanComponent
public class MassNumberProperty extends AbstractNumberProperty {

	@Column(name = "mass_value")
	@BeanColumn(format = "%.4f")
	protected Double value;

	@Column(name = "mass_source", length = JpaPreferences.LENGTH_SOURCE)
	@BeanColumn(i18nId = I18nKeys.SI_MODEL_SOURCE)
	protected String source;


	@Column(name = "mass_editor", length = JpaPreferences.LENGTH_EDITOR)
	protected String editor;

	@Column(name = "mass_additional", length = JpaPreferences.LENGTH_ADDITIONAL)
	@BeanColumn(i18nId = I18nKeys.SI_MODEL_ADDITIONAL)
	protected String additional;


	@Column(name = "mass_lastmodified")
	@Temporal(value = TemporalType.DATE)
	@BeanColumn(i18nId = I18nKeys.SI_MODEL_LAST_DATE, format = "TT:MM:YYYY")
	private Date lastModified;

	/**
	 * Instantiates a new mass number property.
	 */
	public MassNumberProperty() {
	}

	@Override
	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	@Override
	public Double getPh() {
		return null;
	}

	@Override
	public void setPh(Double ph) {

	}

	@Override
	public Integer getCharge() {
		return null;
	}

	@Override
	public void setCharge(Integer charge) {

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
		if (!(o instanceof MassNumberProperty)) return false;
		MassNumberProperty that = (MassNumberProperty) o;
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
