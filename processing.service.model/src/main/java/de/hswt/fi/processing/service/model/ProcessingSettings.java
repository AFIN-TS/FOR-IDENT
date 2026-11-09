package de.hswt.fi.processing.service.model;

import de.hswt.fi.beans.annotations.BeanColumn;
import de.hswt.fi.beans.annotations.BeanComponent;
import de.hswt.fi.common.Ionisation;
import de.hswt.fi.common.StationaryPhase;

import java.io.Serializable;
import java.util.Objects;

@BeanComponent
public class ProcessingSettings implements Serializable {

	private static final long serialVersionUID = 1L;

	@BeanColumn(selector = true, i18nId = I18nKeys.TARGET_PH, format = "%.4f")
	private double ph;

	@BeanColumn(selector = true, i18nId = I18nKeys.PPM, format = "%.4f")
	private double precursorPpm;

	// Bean column needs setter, therefor this constant is not declared static final
	@BeanColumn(selector = true, i18nId = I18nKeys.FORMULA_DERIVED_MASSES_PPM, format = "%.4f")
	private double formulaDerivedMassesPpm = 0.01;

	// Whether the masses calculated from the formulae of the file are used (topic T9); otherwise the precursor m/z
	@BeanColumn(selector = true, i18nId = I18nKeys.USE_FORMULAE)
	private boolean useFormulae = true;

	private double requestedPrecursorPpm;

	@BeanColumn(selector = true, i18nId = I18nKeys.PPM_FRAGMENTS, format = "%.4f")
	private double ppmFragments;

	@BeanColumn(selector = true, i18nId = I18nKeys.INTENSITY_THRESHOLD, format = "%.4f")
	private double intensityThreshold;

	@BeanColumn(selector = true, i18nId = I18nKeys.IONISATION)
	private Ionisation ionisation = Ionisation.NEUTRAL_IONISATION;

	@BeanColumn(selector = true, i18nId = I18nKeys.STATIONARY_PHASE)
	private StationaryPhase stationaryPhase = StationaryPhase.C18;

	private ScoreSettings scoreSettings;

	public ProcessingSettings() {
		precursorPpm = 0.01;
		requestedPrecursorPpm = 5;
		ppmFragments = 5;
		intensityThreshold = 0;
		ph = 3;
		scoreSettings = new ScoreSettings();
	}

	public double getPrecursorPpm() {
		return precursorPpm;
	}

	public double getFormulaDerivedMassesPpm() {
		return formulaDerivedMassesPpm;
	}

	public void setFormulaDerivedMassesPpm(double ppm) {
		this.formulaDerivedMassesPpm = ppm;
	}

	public void setPrecursorPpm(double precursorPpm) {
		this.precursorPpm = precursorPpm;
	}

	public boolean isUseFormulae() {
		return useFormulae;
	}

	public void setUseFormulae(boolean useFormulae) {
		this.useFormulae = useFormulae;
	}

	public double getRequestedPrecursorPpm() {
		return requestedPrecursorPpm;
	}

	public void setRequestedPrecursorPpm(double requestedPrecursoPpm) {
		this.requestedPrecursorPpm = requestedPrecursoPpm;
	}

	public double getPpmFragments() {
		return ppmFragments;
	}

	public void setPpmFragments(double ppm) {
		ppmFragments = ppm;
	}

	public double getIntensityThreshold() {
		return intensityThreshold;
	}

	public void setIntensityThreshold(double intensityThreshold) {
		this.intensityThreshold = intensityThreshold;
	}

	public Ionisation getIonisation() {
		return ionisation;
	}

	public void setIonisation(Ionisation ionisation) {
		this.ionisation = ionisation;
	}

	public double getPh() {
		return ph;
	}

	public void setPh(double ph) {
		this.ph = ph;
	}

	public StationaryPhase getStationaryPhase() {
		return stationaryPhase;
	}

	public void setStationaryPhase(StationaryPhase stationaryPhase) {
		this.stationaryPhase = stationaryPhase;
	}

	public ScoreSettings getScoreSettings() {
		return scoreSettings;
	}

	public void setScoreSettings(ScoreSettings scoreSettings) {
		this.scoreSettings = scoreSettings;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ProcessingSettings that = (ProcessingSettings) o;
		return Double.compare(that.ph, ph) == 0
				&& Double.compare(that.precursorPpm, precursorPpm) == 0
				&& Double.compare(that.formulaDerivedMassesPpm, formulaDerivedMassesPpm) == 0
				&& useFormulae == that.useFormulae
				&& Double.compare(that.requestedPrecursorPpm, requestedPrecursorPpm) == 0
				&& Double.compare(that.ppmFragments, ppmFragments) == 0
				&& Double.compare(that.intensityThreshold, intensityThreshold) == 0
				&& ionisation == that.ionisation
				&& stationaryPhase == that.stationaryPhase
				&& scoreSettings.equals(that.scoreSettings);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ph, precursorPpm, formulaDerivedMassesPpm, useFormulae, requestedPrecursorPpm, ppmFragments,
				intensityThreshold, ionisation, stationaryPhase, scoreSettings);
	}

	@Override
	public String toString() {
		return "ProcessSettings [precursorPpm=" + precursorPpm + ", requestedPrecursorPpm="
				+ requestedPrecursorPpm + ", ppmFragments=" + ppmFragments + ",  intensityThreshold="
				+ intensityThreshold + ", "
				+ (ionisation != null ? "ionisation=" + ionisation + ", " : "") + "ph=" + ph + ", "
				+ (stationaryPhase != null ? "stationaryPhase=" + stationaryPhase + ", " : "")
				+ (scoreSettings != null ? "scoreSettings=" + scoreSettings : "") + "]";
	}
}
