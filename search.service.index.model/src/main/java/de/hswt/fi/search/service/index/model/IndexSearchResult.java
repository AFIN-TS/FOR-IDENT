package de.hswt.fi.search.service.index.model;

import de.hswt.fi.beans.annotations.BeanColumn;
import de.hswt.fi.beans.annotations.BeanComponent;
import de.hswt.fi.model.Score;
import de.hswt.fi.search.service.mass.search.model.Entry;
import java.util.Objects;

@BeanComponent
public class IndexSearchResult {
	@BeanColumn(selector = true, i18nId = "RTI_MODEL_SCORE")
	protected Score score;
	@BeanColumn(selector = true, i18nId = "RTI_MODEL_TARGET_IDENTIFIER")
	protected String targetIdentifier;
	@BeanColumn(format = "%.4f", selector = true, i18nId = "RTI_MODEL_TARGET_MASS")
	protected double targetMass;
	@BeanColumn(format = "%.2f", selector = true, i18nId = "RTI_MODEL_TARGET_RT")
	protected Double retentionTime;
	@BeanColumn(format = "%.2f", selector = true, i18nId = "RTI_MODEL_RTI")
	protected Double retentionTimeIndex;
	@BeanColumn(format = "%.4f", selector = true, i18nId = "RTI_MODEL_DELTA_MASS")
	protected double deltaMass;
	@BeanColumn(selector = true, i18nId = "RTI_MODEL_PH_DEPENDENCY", i18nValuePropertyId = "i18nKey")
	private MoleculePhDependency moleculePhDependency;
	@BeanColumn(format = "%.2f", selector = true, i18nId = "RTI_MODEL_TARGET_LOG_D")
	private Double targetLogD;
	@BeanColumn(format = "%.1f", selector = true, i18nId = "RTI_MODEL_TARGET_PH")
	private double targetPh;
	private boolean calculatedTargetMass;
	@BeanColumn(format = "%.2f", selector = true, i18nId = "RTI_MODEL_RESULT_LOG_D")
	private Double resultLogD;
	@BeanColumn(format = "%.2f", selector = true, i18nId = "RTI_MODEL_DELTA_LOG_D")
	private Double deltaLogDRtiDb;
	@BeanColumn(format = "%.2f", selector = true, i18nId = "RTI_MODEL_ADJUSTED_LOG_D")
	private Double adjustedLogD;
	@BeanColumn(format = "%.2f", selector = true, i18nId = "RTI_MODEL_ADJUSTED_DELTA_LOG_D")
	private Double deltaLogDAdjustedDb;
	private double ionisation = 0.0;
	private String stationaryPhase;
	@BeanColumn(format = "%.2f", selector = true, i18nId = "RTI_SIGNAL_MODEL_RTI")
	protected Double retentionTimeSignal;
	@BeanColumn(format = "%.2f", selector = true, i18nId = "RTI_DELTA_SIGNAL_MODEL_RTI")
	protected Double deltaRetentionTimeSignal;
	protected Entry entry;
	protected double ppm;
	protected boolean isFirst;
	protected boolean isLast;
	protected boolean available;

	private IndexSearchResult() {
	}

	/**
	 * Result for a candidate without RTI result, e.g. without Henry constant or with a retention time outside the
	 * calibration: no values, not available. The processing sets the target identifier and the entry afterwards.
	 * (Until SP-16 the services built it with a constructor that rejects a missing entry, which crashed the GC RTI
	 * screening.)
	 */
	public static IndexSearchResult notAvailable() {
		IndexSearchResult result = new IndexSearchResult();
		result.targetIdentifier = "";
		result.targetMass = Double.NaN;
		result.deltaMass = Double.NaN;
		result.retentionTime = Double.NaN;
		result.retentionTimeIndex = Double.NaN;
		result.targetPh = Double.NaN;
		result.ppm = Double.NaN;
		result.available = false;
		return result;
	}

	public IndexSearchResult(
		String targetIdentifier,
		Entry entry,
		Double targetRetentionTime,
		Double targetPh,
		Double targetLogD,
		Double targetMass,
		boolean calculatedTargetMass,
		Double adjustedLogD,
		Double resultLogD,
		Double ppm,
		Double rti,
		Double ionisation,
		String stationaryPhase,
		MoleculePhDependency dependency
	) {
		Objects.requireNonNull(entry);
		this.targetIdentifier = targetIdentifier;
		this.retentionTime = targetRetentionTime;
		this.entry = entry;
		this.targetLogD = targetLogD;
		this.targetMass = targetMass;
		this.calculatedTargetMass = calculatedTargetMass;
		this.adjustedLogD = adjustedLogD;
		this.resultLogD = resultLogD;
		this.ppm = ppm;
		this.targetPh = targetPh;
		this.retentionTimeIndex = rti;
		this.ionisation = ionisation;
		this.stationaryPhase = stationaryPhase;
		this.targetIdentifier = targetIdentifier;
		this.moleculePhDependency = dependency;
		if (entry.getAccurateMass() != null) {
			this.deltaMass = entry.getAccurateMass().getValue() - targetMass;
		}

		if (entry.getHenryBond() != null && entry.getHenryBond().getValue() != null && this.retentionTimeSignal != null) {
			this.deltaRetentionTimeSignal = entry.getHenryBond().getValue() - this.retentionTimeSignal;
			this.available = true;
		}

		if (this.targetLogD == null) {
			this.available = false;
		} else {
			this.available = true;
			this.deltaLogDRtiDb = targetLogD - resultLogD;
			this.deltaLogDAdjustedDb = adjustedLogD - resultLogD;
		}
	}

	public IndexSearchResult(
		String targetIdentifier, Entry entry, Double retentionTime, Double targetMass, Double ppm, Double retentionTimeIndex, Double retentionTimeSignal
	) {
		Objects.requireNonNull(entry);
		this.targetIdentifier = targetIdentifier;
		this.retentionTime = retentionTime;
		this.entry = entry;
		this.targetMass = targetMass;
		this.ppm = ppm;
		this.retentionTimeIndex = retentionTimeIndex;
		this.retentionTimeSignal = retentionTimeSignal;
		if (entry.getAccurateMass() != null) {
			this.deltaMass = entry.getAccurateMass().getValue() - targetMass;
		}

		if (entry.getHenryBond() != null && entry.getHenryBond().getValue() != null && retentionTimeSignal != null) {
			this.deltaRetentionTimeSignal = entry.getHenryBond().getValue() - retentionTimeSignal;
			this.available = true;
		}
	}

	public String getID() {
		return this.targetIdentifier + this.entry.getPublicID();
	}

	public Score getScore() {
		return this.score;
	}

	public void setScore(Score score) {
		this.score = score;
	}

	public String getTargetIdentifier() {
		return this.targetIdentifier;
	}

	public void setTargetIdentifier(String targetIdentifier) {
		this.targetIdentifier = targetIdentifier;
	}

	public double getTargetMass() {
		return this.targetMass;
	}

	public void setTargetMass(double targetMass) {
		this.targetMass = targetMass;
	}

	public Double getRetentionTime() {
		return this.retentionTime;
	}

	public void setRetentionTime(Double retentionTime) {
		this.retentionTime = retentionTime;
	}

	public Double getRetentionTimeIndex() {
		return this.retentionTimeIndex;
	}

	public void setRetentionTimeIndex(Double retentionTimeIndex) {
		this.retentionTimeIndex = retentionTimeIndex;
	}

	public double getDeltaMass() {
		return this.deltaMass;
	}

	public void setDeltaMass(double deltaMass) {
		this.deltaMass = deltaMass;
	}

	public MoleculePhDependency getMoleculePhDependency() {
		return this.moleculePhDependency;
	}

	public void setMoleculePhDependency(MoleculePhDependency moleculePhDependency) {
		this.moleculePhDependency = moleculePhDependency;
	}

	public Double getTargetLogD() {
		return this.targetLogD;
	}

	public void setTargetLogD(Double targetLogD) {
		this.targetLogD = targetLogD;
	}

	public double getTargetPh() {
		return this.targetPh;
	}

	public void setTargetPh(double targetPh) {
		this.targetPh = targetPh;
	}

	public boolean isCalculatedTargetMass() {
		return this.calculatedTargetMass;
	}

	public void setCalculatedTargetMass(boolean calculatedTargetMass) {
		this.calculatedTargetMass = calculatedTargetMass;
	}

	public Double getResultLogD() {
		return this.resultLogD;
	}

	public void setResultLogD(Double resultLogD) {
		this.resultLogD = resultLogD;
	}

	public Double getDeltaLogDRtiDb() {
		return this.deltaLogDRtiDb;
	}

	public void setDeltaLogDRtiDb(Double deltaLogDRtiDb) {
		this.deltaLogDRtiDb = deltaLogDRtiDb;
	}

	public Double getAdjustedLogD() {
		return this.adjustedLogD;
	}

	public void setAdjustedLogD(Double adjustedLogD) {
		this.adjustedLogD = adjustedLogD;
	}

	public Double getDeltaLogDAdjustedDb() {
		return this.deltaLogDAdjustedDb;
	}

	public void setDeltaLogDAdjustedDb(Double deltaLogDAdjustedDb) {
		this.deltaLogDAdjustedDb = deltaLogDAdjustedDb;
	}

	public double getIonisation() {
		return this.ionisation;
	}

	public void setIonisation(double ionisation) {
		this.ionisation = ionisation;
	}

	public String getStationaryPhase() {
		return this.stationaryPhase;
	}

	public void setStationaryPhase(String stationaryPhase) {
		this.stationaryPhase = stationaryPhase;
	}

	public Double getRetentionTimeSignal() {
		return this.retentionTimeSignal;
	}

	public void setRetentionTimeSignal(Double retentionTimeSignal) {
		this.retentionTimeSignal = retentionTimeSignal;
	}

	public Double getDeltaRetentionTimeSignal() {
		return this.deltaRetentionTimeSignal;
	}

	public void setDeltaRetentionTimeSignal(Double deltaRetentionTimeSignal) {
		this.deltaRetentionTimeSignal = deltaRetentionTimeSignal;
	}

	public Entry getEntry() {
		return this.entry;
	}

	public void setEntry(Entry entry) {
		this.entry = entry;
	}

	public double getPpm() {
		return this.ppm;
	}

	public void setPpm(double ppm) {
		this.ppm = ppm;
	}

	public boolean isFirst() {
		return this.isFirst;
	}

	public void setFirst(boolean first) {
		this.isFirst = first;
	}

	public boolean isLast() {
		return this.isLast;
	}

	public void setLast(boolean last) {
		this.isLast = last;
	}

	public boolean isAvailable() {
		return this.available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		IndexSearchResult that = (IndexSearchResult) o;
		return Double.compare(that.targetMass, this.targetMass) == 0
				&& Double.compare(that.deltaMass, this.deltaMass) == 0
				&& Double.compare(that.targetPh, this.targetPh) == 0
				&& this.calculatedTargetMass == that.calculatedTargetMass
				&& Double.compare(that.ionisation, this.ionisation) == 0
				&& Double.compare(that.ppm, this.ppm) == 0
				&& this.isFirst == that.isFirst
				&& this.isLast == that.isLast
				&& this.available == that.available
				&& Objects.equals(this.score, that.score)
				&& Objects.equals(this.targetIdentifier, that.targetIdentifier)
				&& Objects.equals(this.retentionTime, that.retentionTime)
				&& Objects.equals(this.retentionTimeIndex, that.retentionTimeIndex)
				&& this.moleculePhDependency == that.moleculePhDependency
				&& Objects.equals(this.targetLogD, that.targetLogD)
				&& Objects.equals(this.resultLogD, that.resultLogD)
				&& Objects.equals(this.deltaLogDRtiDb, that.deltaLogDRtiDb)
				&& Objects.equals(this.adjustedLogD, that.adjustedLogD)
				&& Objects.equals(this.deltaLogDAdjustedDb, that.deltaLogDAdjustedDb)
				&& Objects.equals(this.stationaryPhase, that.stationaryPhase)
				&& Objects.equals(this.retentionTimeSignal, that.retentionTimeSignal)
				&& Objects.equals(this.deltaRetentionTimeSignal, that.deltaRetentionTimeSignal)
				&& Objects.equals(this.entry, that.entry);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			this.score,
			this.targetIdentifier,
			this.targetMass,
			this.retentionTime,
			this.retentionTimeIndex,
			this.deltaMass,
			this.moleculePhDependency,
			this.targetLogD,
			this.targetPh,
			this.calculatedTargetMass,
			this.resultLogD,
			this.deltaLogDRtiDb,
			this.adjustedLogD,
			this.deltaLogDAdjustedDb,
			this.ionisation,
			this.stationaryPhase,
			this.retentionTimeSignal,
			this.deltaRetentionTimeSignal,
			this.entry,
			this.ppm,
			this.isFirst,
			this.isLast,
			this.available
		);
	}

	@Override
	public String toString() {
		return "IndexSearchResult{score="
			+ this.score
			+ ", targetIdentifier='"
			+ this.targetIdentifier
			+ '\''
			+ ", targetMass="
			+ this.targetMass
			+ ", retentionTime="
			+ this.retentionTime
			+ ", retentionTimeIndex="
			+ this.retentionTimeIndex
			+ ", deltaMass="
			+ this.deltaMass
			+ ", moleculePhDependency="
			+ this.moleculePhDependency
			+ ", targetLogD="
			+ this.targetLogD
			+ ", targetPh="
			+ this.targetPh
			+ ", calculatedTargetMass="
			+ this.calculatedTargetMass
			+ ", resultLogD="
			+ this.resultLogD
			+ ", deltaLogDRtiDb="
			+ this.deltaLogDRtiDb
			+ ", adjustedLogD="
			+ this.adjustedLogD
			+ ", deltaLogDAdjustedDb="
			+ this.deltaLogDAdjustedDb
			+ ", ionisation="
			+ this.ionisation
			+ ", stationaryPhase='"
			+ this.stationaryPhase
			+ '\''
			+ ", retentionTimeSignal="
			+ this.retentionTimeSignal
			+ ", deltaRetentionTimeSignal="
			+ this.deltaRetentionTimeSignal
			+ ", entry="
			+ this.entry
			+ ", ppm="
			+ this.ppm
			+ ", isFirst="
			+ this.isFirst
			+ ", isLast="
			+ this.isLast
			+ ", available="
			+ this.available
			+ '}';
	}
}
