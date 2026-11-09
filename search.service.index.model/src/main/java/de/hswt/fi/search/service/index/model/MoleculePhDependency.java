package de.hswt.fi.search.service.index.model;

public enum MoleculePhDependency {

	NEUTRAL(I18nKeys.RTI_MODEL_CHARGE_NEUTRAL),
	NEUTRAL_UPPER(I18nKeys.RTI_MODEL_CHARGE_NEUTRAL_UPPER),
	NEUTRAL_LOWER(I18nKeys.RTI_MODEL_CHARGE_NEUTRAL_LOWER),
	POSITIVE_LOADABLE(I18nKeys.RTI_MODEL_CHARGE_POSITIVE_LOADED),
	NEGATIVE_LOADABLE(I18nKeys.RTI_MODEL_CHARGE_NEGATIVE_LOADED);

	private String i18nKey;

	MoleculePhDependency(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	public String getI18nKey() {
		return i18nKey;
	}
}
