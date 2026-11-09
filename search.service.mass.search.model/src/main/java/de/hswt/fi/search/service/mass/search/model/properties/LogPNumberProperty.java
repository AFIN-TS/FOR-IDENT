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
 * The Class LogPNumberProperty represents the logP number property in the DB.
 *
 * @author Marco Luthardt
 */
@Entity
@Table(name = JpaPreferences.TABLE_NAME_LOGP)
@BeanComponent
public class LogPNumberProperty extends AbstractNumberProperty {

	@Id
	@GeneratedValue
	private Long id;

	@Column
	@BeanColumn(format = "%.2f", i18nId = I18nKeys.SI_MODEL_LOG_P)
	protected Double value;

	@Column
	@BeanColumn(i18nId = I18nKeys.SI_MODEL_SOURCE)
	protected String source;


	@Column
	protected String editor;

	@Column(length = JpaPreferences.LENGTH_ADDITIONAL)
	@BeanColumn(i18nId = I18nKeys.SI_MODEL_ADDITIONAL)
	protected String additional;


	@Column
	@Temporal(value = TemporalType.DATE)
	@BeanColumn(i18nId = I18nKeys.SI_MODEL_LAST_DATE, format = "TT:MM:YYYY")
	private Date lastModified;

	@JsonIgnore
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Entry substance;

	/**
	 * Instantiates a new log p number property.
	 */
	public LogPNumberProperty() {
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

	public void setSubstance(Entry substance) {
		this.substance = substance;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof LogPNumberProperty)) return false;
		LogPNumberProperty that = (LogPNumberProperty) o;
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
