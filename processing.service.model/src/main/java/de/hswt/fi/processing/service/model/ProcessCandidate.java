package de.hswt.fi.processing.service.model;

import de.hswt.fi.beans.annotations.BeanColumn;
import de.hswt.fi.beans.annotations.BeanComponent;
import de.hswt.fi.model.Score;
import de.hswt.fi.msms.service.model.MsMsCandidate;
import de.hswt.fi.search.service.index.model.IndexSearchResult;
import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.MassSearchResult;
import de.hswt.fi.search.service.tp.model.PathwayCandidate;

@BeanComponent
public class ProcessCandidate {

	@BeanColumn(selector = true, i18nId = I18nKeys.PROCESS_MODEL_BEST_MATCH)
	private boolean bestMatch;

	@BeanColumn(selector = true, i18nId = I18nKeys.PROCESS_MODEL_SCORE)
	private Score score;

	@BeanColumn(selector = true, i18nId = I18nKeys.PROCESS_MODEL_MASSBANK_SIMPLE_SCORE)
	private Score massBankSimpleScore;

	@BeanColumn
	private MsMsCandidate msMsCandidate = null;

	@BeanColumn
	private MassSearchResult massSearchResult = null;

	@BeanColumn
	private IndexSearchResult indexSearchResult = null;

	@BeanColumn
	private PathwayCandidate pathwayCandidate = null;

	private String id;

	private String targetIdentifier;

	private Double retentionTime;

	public ProcessCandidate(MassSearchResult result) {
		massSearchResult = result;
		id = result.getID();
		targetIdentifier = result.getTargetIdentifier();
		retentionTime = result.getTargetRetentionTime();
	}

	public String getTargetIdentifier() {
		return targetIdentifier;
	}

	public String getId() {
		return id;
	}

	public MsMsCandidate getMsMsCandidate() {
		return msMsCandidate;
	}

	public void setMsMsCandidate(MsMsCandidate msMsCandidate) {
		this.msMsCandidate = msMsCandidate;
	}

	public void setScore(Score score) {
		this.score = score;
	}

	public Score getScore() {
		return score;
	}

	public void setMassBankSimpleScore(Score massBankSimpleScore) {
		this.massBankSimpleScore = massBankSimpleScore;
	}

	public Score getMassBankSimpleScore() {
		return massBankSimpleScore;
	}

	public MassSearchResult getMassSearchResult() {
		return massSearchResult;
	}

	public void setMassSearchResult(MassSearchResult massSearchResult) {
		this.massSearchResult = massSearchResult;
	}

	public IndexSearchResult getIndexSearchResult() {
		return indexSearchResult;
	}

	public void setIndexSearchResult(IndexSearchResult indexSearchResult) {
		this.indexSearchResult = indexSearchResult;
	}

	public void setBestMatch(boolean bestMatch) {
		this.bestMatch = bestMatch;
	}

	public boolean isBestMatch() {
		return bestMatch;
	}

	public Double getRetentionTime() {
		return retentionTime;
	}

	public PathwayCandidate getPathwayCandidate() {
		return pathwayCandidate;
	}

	public void setPathwayCandidate(PathwayCandidate pathwayCandidate) {
		this.pathwayCandidate = pathwayCandidate;
	}

	public Entry getEntry() {
		if (massSearchResult != null) {
			return massSearchResult.getEntry();
		} else {
			return indexSearchResult.getEntry();
		}
	}
}
