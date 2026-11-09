package de.hswt.fi.search.service.mass.search.model;

import de.hswt.fi.beans.annotations.BeanColumn;
import de.hswt.fi.beans.annotations.BeanComponent;
import de.hswt.fi.search.service.mass.search.model.properties.*;
import org.eclipse.persistence.annotations.BatchFetch;
import org.eclipse.persistence.annotations.BatchFetchType;

import jakarta.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = JpaPreferences.TABLE_NAME_ENTRY)
@BeanComponent
public class Entry {

	@Id
	@GeneratedValue
	private Long id;

	@Column
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_PUBLIC_ID, caption="STOFFIDENT-ID")
	private String publicID;

	@Embedded
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_NAME)
	private NameStringProperty name;

	@Embedded
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_INCHI)
	private InchiStringProperty inchi;

	@Embedded
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_INCHI_KEY)
	private InchiKeyStringProperty inchiKey;

	@Embedded
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_CAS)
	private CasStringProperty cas;

	@Embedded
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_EC_NUMBER)
	private ECNumberStringProperty ecNumber;

	@Embedded
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_SMILES)
	private SMILESStringProperty smiles;

	@Embedded
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_FORMULA)
	private FormulaStringProperty elementalFormula;

	@Embedded
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_IUPAC)
	private IUPACStringProperty iupac;

	@Embedded
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_MASS)
	private MassNumberProperty accurateMass;

	@Embedded
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_TONNAGE)
	private TonnageStringProperty tonnage;

	@Embedded
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_HENRY_BOND)
	private HenryConstantBondNumberProperty henryBond;

	@Embedded
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_HENRY_GROUP)
	private HenryConstantGroupNumberProperty henryGroup;

	@Embedded
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_HENRY_EXPER)
	private HenryConstantExperNumberProperty henryExper;

	@Column
	@Temporal(value = TemporalType.DATE)
	private Date lastModified;

	@OneToMany(mappedBy = "substance", cascade = CascadeType.ALL, orphanRemoval = true)
	@BatchFetch(value = BatchFetchType.JOIN)
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_LOG_P)
	private Set<LogPNumberProperty> logpValues;

	@OneToMany(mappedBy = "substance", cascade = CascadeType.ALL, orphanRemoval = true)
	@BatchFetch(value = BatchFetchType.JOIN)
	@OrderBy("ph ASC")
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_LOG_D)
	private Set<LogDNumberProperty> logdValues;

	@OneToMany(mappedBy = "substance", cascade = CascadeType.ALL, orphanRemoval = true)
	@BatchFetch(value = BatchFetchType.JOIN)
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_ADDITIONAL_NAMES)
	private Set<AdditionalNameStringProperty> additionalNames;

	@OneToMany(mappedBy = "substance", cascade = CascadeType.ALL, orphanRemoval = true)
	@BatchFetch(value = BatchFetchType.JOIN)
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_MASSBANK_IDS)
	private Set<MassBankIdStringProperty> massBankIds;

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@BatchFetch(value = BatchFetchType.JOIN)
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_CATEGROIES)
	private Set<CategoryStringProperty> categories;

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@BatchFetch(value = BatchFetchType.JOIN)
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_SOURCE_LISTS)
	private Set<SourceList> sourceLists;

	@Column
	@BeanColumn(selector = true, i18nId = I18nKeys.SI_MODEL_DATA_SOURCE_NAME)
	private String datasourceName;

	@Column
	private String dtxsid;

	public StringValueProperty getName() {
		return name;
	}

	public StringValueProperty getCas() {
		return cas;
	}

	public StringValueProperty getEcNumber() {
		return ecNumber;
	}

	public StringValueProperty getSmiles() {
		return smiles;
	}

	public StringValueProperty getElementalFormula() {
		return elementalFormula;
	}

	public StringValueProperty getIupac() {
		return iupac;
	}

	public NumberValueProperty getAccurateMass() {
		return accurateMass;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public StringValueProperty getTonnage() {
		return tonnage;
	}

	public HenryConstantBondNumberProperty getHenryBond() {
		return henryBond;
	}

	public HenryConstantGroupNumberProperty getHenryGroup() {
		return henryGroup;
	}

	public HenryConstantExperNumberProperty getHenryExper() {
		return henryExper;
	}

	public Set<NumberValueProperty> getLogpValues() {
		return logpValues.stream().filter(Objects::nonNull).sorted(Comparator.comparingDouble(LogPNumberProperty::getValue))
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public Set<NumberValueProperty> getLogdValues() {
		return logdValues.stream().filter(Objects::nonNull).filter(logD -> logD.getPh() != null)
				.sorted(Comparator.comparingDouble(LogDNumberProperty::getPh))
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public Set<StringValueProperty> getAdditionalNames() {
		return additionalNames.stream().filter(Objects::nonNull).sorted(AbstractStringProperty::compareTo)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public Set<StringValueProperty> getMassBankIds() {
		return massBankIds.stream().filter(Objects::nonNull).sorted(AbstractStringProperty::compareTo)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public Set<CategoryStringProperty> getCategories() {
		return categories.stream().filter(Objects::nonNull).sorted(AbstractStringProperty::compareTo)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public Set<SourceList> getSourceLists() {
		return sourceLists.stream().filter(Objects::nonNull).sorted(Comparator.comparing(SourceList::getDescription))
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public String getPublicID() {
		return publicID;
	}

	public InchiStringProperty getInchi() {
		return inchi;
	}

	public InchiKeyStringProperty getInchiKey() {
		return inchiKey;
	}

	public String getDatasourceName() {
		return datasourceName;
	}

	public void setDatasourceName(String databaseName) {
		this.datasourceName = databaseName;
	}

	public String getDtxsid() {
		return dtxsid;
	}

	public void addSourceList(SourceList sourceList) {
		sourceLists.add(sourceList);
	}

	public void addCategory(CategoryStringProperty category) {
		categories.add(category);
	}

	@Override
	public String toString() {
		return "Entry [entryID=" + id + ", publicID=" + publicID + ", inchi=" + inchi
				+ ", inchiKey=" + inchiKey + ", cas=" + cas + ", name=" + name + ", ecNumber="
				+ ecNumber + ", smiles=" + smiles + ", elementalFormula=" + elementalFormula
				+ ", iupac=" + iupac + ", accurateMass=" + accurateMass + ", tonnage=" + tonnage
				+ ", lastModified=" + lastModified + ", logpValues=" + logpValues + ", logdValues="
				+ logdValues + ", additionalNames=" + additionalNames + ", massBankIds="
				+ massBankIds + ", categories=" + categories + ", sourceLists=" + sourceLists + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Entry entry = (Entry) o;
		return Objects.equals(publicID, entry.publicID) &&
				Objects.equals(name, entry.name) &&
				Objects.equals(inchi, entry.inchi) &&
				Objects.equals(inchiKey, entry.inchiKey) &&
				Objects.equals(cas, entry.cas) &&
				Objects.equals(ecNumber, entry.ecNumber) &&
				Objects.equals(smiles, entry.smiles) &&
				Objects.equals(elementalFormula, entry.elementalFormula) &&
				Objects.equals(iupac, entry.iupac) &&
				Objects.equals(accurateMass, entry.accurateMass) &&
				Objects.equals(tonnage, entry.tonnage) &&
				Objects.equals(henryBond, entry.henryBond) &&
				Objects.equals(henryGroup, entry.henryGroup) &&
				Objects.equals(henryExper, entry.henryExper) &&
				Objects.equals(lastModified, entry.lastModified) &&
				Objects.equals(logpValues, entry.logpValues) &&
				Objects.equals(logdValues, entry.logdValues) &&
				Objects.equals(additionalNames, entry.additionalNames) &&
				Objects.equals(massBankIds, entry.massBankIds) &&
				Objects.equals(categories, entry.categories) &&
				Objects.equals(sourceLists, entry.sourceLists) &&
				Objects.equals(datasourceName, entry.datasourceName) &&
				Objects.equals(dtxsid, entry.dtxsid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(publicID, name, inchi, inchiKey, cas, ecNumber, smiles, elementalFormula, iupac, accurateMass, tonnage, henryBond, henryGroup, henryExper, lastModified, logpValues, logdValues, additionalNames, massBankIds, categories, sourceLists, datasourceName, dtxsid);
	}

	@SuppressWarnings("UnusedReturnValue")
	public static final class EntryBuilder {
		private String publicID;
		private NameStringProperty name;
		private InchiStringProperty inchi;
		private InchiKeyStringProperty inchiKey;
		private CasStringProperty cas;
		private ECNumberStringProperty ecNumber;
		private SMILESStringProperty smiles;
		private FormulaStringProperty elementalFormula;
		private IUPACStringProperty iupac;
		private MassNumberProperty accurateMass;
		private TonnageStringProperty tonnage;
		private HenryConstantBondNumberProperty henryBond;
		private HenryConstantGroupNumberProperty henryGroup;
		private HenryConstantExperNumberProperty henryExper;
		private Date lastModified;
		private Set<LogPNumberProperty> logpValues;
		private Set<LogDNumberProperty> logdValues;
		private Set<AdditionalNameStringProperty> additionalNames;
		private Set<MassBankIdStringProperty> massBankIds;
		private Set<CategoryStringProperty> categories;
		private Set<SourceList> sourceLists;
		private String dtxsid;

		private EntryBuilder() {
			logpValues = new LinkedHashSet<>();
			logdValues = new LinkedHashSet<>();
			additionalNames = new LinkedHashSet<>();
			massBankIds = new LinkedHashSet<>();
			categories = new LinkedHashSet<>();
			sourceLists = new LinkedHashSet<>();
		}

		public static EntryBuilder newBuilder() {
			return new EntryBuilder();
		}

		public EntryBuilder withPublicID(String publicID) {
			this.publicID = publicID;
			return this;
		}

		public EntryBuilder withName(NameStringProperty name) {
			this.name = name;
			return this;
		}

		public EntryBuilder withInchi(InchiStringProperty inchi) {
			this.inchi = inchi;
			return this;
		}

		public EntryBuilder withInchiKey(InchiKeyStringProperty inchiKey) {
			this.inchiKey = inchiKey;
			return this;
		}

		public EntryBuilder withCas(CasStringProperty cas) {
			this.cas = cas;
			return this;
		}

		public EntryBuilder withEcNumber(ECNumberStringProperty ecNumber) {
			this.ecNumber = ecNumber;
			return this;
		}

		public EntryBuilder withSmiles(SMILESStringProperty smiles) {
			this.smiles = smiles;
			return this;
		}

		public EntryBuilder withElementalFormula(FormulaStringProperty elementalFormula) {
			this.elementalFormula = elementalFormula;
			return this;
		}

		public EntryBuilder withIupac(IUPACStringProperty iupac) {
			this.iupac = iupac;
			return this;
		}

		public EntryBuilder withAccurateMass(MassNumberProperty accurateMass) {
			this.accurateMass = accurateMass;
			return this;
		}

		public EntryBuilder withTonnage(TonnageStringProperty tonnage) {
			this.tonnage = tonnage;
			return this;
		}

		public EntryBuilder withHenryBond(HenryConstantBondNumberProperty henryBond) {
			this.henryBond = henryBond;
			return this;
		}

		public EntryBuilder withHenryGroup(HenryConstantGroupNumberProperty henryGroup) {
			this.henryGroup = henryGroup;
			return this;
		}

		public EntryBuilder withHenryExper(HenryConstantExperNumberProperty henryExper) {
			this.henryExper = henryExper;
			return this;
		}

		public EntryBuilder withLastModified(Date lastModified) {
			this.lastModified = lastModified;
			return this;
		}

		public EntryBuilder withLogPValue(LogPNumberProperty logpValue) {
			if (logpValue != null) {
				logpValues.add(logpValue);
			}
			return this;
		}

		public EntryBuilder withLogDValue(LogDNumberProperty logdValue) {
			if (logdValue != null) {
				logdValues.add(logdValue);
			}
			return this;
		}

		public EntryBuilder withAdditionalName(AdditionalNameStringProperty additionalName) {
			if (additionalName != null) {
				additionalNames.add(additionalName);
			}
			return this;
		}

		public EntryBuilder withMassBankId(MassBankIdStringProperty massBankId) {
			if (massBankId != null) {
				massBankIds.add(massBankId);
			}
			return this;
		}

		public EntryBuilder withCategory(CategoryStringProperty category) {
			if(category != null) {
				categories.add(category);
			}
			return this;
		}

		public EntryBuilder withSourceList(SourceList sourceList) {
			if (sourceList != null) {
				sourceLists.add(sourceList);
			}
			return this;
		}

		public EntryBuilder withDtxsid(String dtxsid) {
			this.dtxsid = dtxsid;
			return this;
		}

		public Entry build() {
			Entry entry = new Entry();
			entry.categories = this.categories;
			entry.cas = this.cas;
			entry.logpValues = this.logpValues;
			entry.logpValues.forEach(value -> value.setSubstance(entry));
			entry.inchiKey = this.inchiKey;
			entry.logdValues = this.logdValues;
			entry.logdValues.forEach(value -> value.setSubstance(entry));
			entry.sourceLists = this.sourceLists;
			entry.inchi = this.inchi;
			entry.additionalNames = this.additionalNames;
			entry.additionalNames.forEach(value -> value.setSubstance(entry));
			entry.publicID = this.publicID;
			entry.name = this.name;
			entry.accurateMass = this.accurateMass;
			entry.lastModified = this.lastModified;
			entry.tonnage = this.tonnage;
			entry.henryBond = this.henryBond;
			entry.henryGroup = this.henryGroup;
			entry.henryExper = this.henryExper;
			entry.smiles = this.smiles;
			entry.iupac = this.iupac;
			entry.ecNumber = this.ecNumber;
			entry.elementalFormula = this.elementalFormula;
			entry.massBankIds = this.massBankIds;
			entry.massBankIds.forEach(value -> value.setSubstance(entry));
			entry.dtxsid = this.dtxsid;
			return entry;
		}
	}
}
