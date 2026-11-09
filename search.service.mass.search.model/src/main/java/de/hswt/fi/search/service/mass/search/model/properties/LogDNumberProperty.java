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
 * The Class LogDNumberProperty represents the logD number property in the DB.
 * It has an own table in the DB.
 *
 * @author Marco Luthardt
 */
@Entity
@Table(name = JpaPreferences.TABLE_NAME_LOGD)
@BeanComponent
public class LogDNumberProperty extends AbstractNumberProperty {


	@Id
	@GeneratedValue
	private Long id;

	@Column
	@BeanColumn(format = "%.2f", i18nId = I18nKeys.SI_MODEL_LOG_D)
	private Double value;

	@Column
	@BeanColumn(format = "%.1f", i18nId = I18nKeys.SI_MODEL_PH)
	private Double ph;

	@Column
	private Integer charge;

	@Column
	@BeanColumn(i18nId = I18nKeys.SI_MODEL_SOURCE)
	private String source;


	@Column
	private String editor;

	@Column(length = JpaPreferences.LENGTH_ADDITIONAL)
	@BeanColumn(i18nId = I18nKeys.SI_MODEL_ADDITIONAL)
	private String additional;


	@Column
	@Temporal(value = TemporalType.DATE)
	@BeanColumn(i18nId = I18nKeys.SI_MODEL_LAST_DATE, format = "TT:MM:YYYY")
	private Date lastModified;

	@JsonIgnore
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Entry substance;

	/**
	 * Instantiates a new log d number property.
	 */
	public LogDNumberProperty() {
	}

	public Long getId() {
		return id;
	}

	@Override
	public Double getValue() {
		return value;
	}

	@Override
	public void setValue(Double value) {
		this.value = value;
	}

	@Override
	public Double getPh() {
		return ph;
	}

	public void setPh(Double ph) {
		this.ph = ph;
	}

	@Override
	public Integer getCharge() {
		return charge;
	}

	public void setCharge(Integer charge) {
		this.charge = charge;
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

	public Entry getSubstance() {
		return substance;
	}

	public void setSubstance(Entry substance) {
		this.substance = substance;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof LogDNumberProperty)) return false;
		LogDNumberProperty that = (LogDNumberProperty) o;
		return Objects.equals(value, that.value) &&
				Objects.equals(ph, that.ph) &&
				Objects.equals(charge, that.charge) &&
				Objects.equals(source, that.source) &&
				Objects.equals(editor, that.editor) &&
				Objects.equals(additional, that.additional) &&
				Objects.equals(lastModified, that.lastModified);
	}

	@Override
	public int hashCode() {

		return Objects.hash(value, ph, charge, source, editor, additional, lastModified);
	}
}
