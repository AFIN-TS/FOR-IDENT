package de.hswt.fi.search.service.search.api;

import de.hswt.fi.common.SearchUtil;
import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.SearchParameter;
import de.hswt.fi.search.service.mass.search.model.SourceList;
import de.hswt.fi.search.service.mass.search.model.WebserviceEntry;
import de.hswt.fi.search.service.mass.search.model.properties.AdditionalNameStringProperty;
import de.hswt.fi.search.service.mass.search.model.properties.CategoryStringProperty;
import de.hswt.fi.search.service.mass.search.model.properties.LogPNumberProperty;
import de.hswt.fi.search.service.mass.search.model.properties.NumberSearchProperty;
import de.hswt.fi.search.service.search.api.repositories.EntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * The Class JPASISearchService is an implementation of the SI search service.
 * The connection to the DB is realized with JPA.
 *
 * @author Marco Luthardt
 * @author August Gilg
 */
public abstract class AbstractCompoundService implements CompoundSearchService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCompoundService.class);

	public abstract EntryRepository getEntryRepository();

	public abstract EntityManager getEntityManager();

	/*
	 * (non-Javadoc)
	 *
	 * @see de.hswt.riskident.si.dao.api.SIDataSearchService#getLastUpdateTime()
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public long getLastUpdateTime() {
		CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();

		CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
		Root entry = criteriaQuery.from(Entry.class);
		criteriaQuery.select(criteriaBuilder.max(entry.get("lastModified")));
		TypedQuery<Date> query = getEntityManager().createQuery(criteriaQuery);

		Date result = null;
		try {
			result = query.getSingleResult();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}

		long time = result != null ? result.getTime() : 0;

		getEntityManager().close();
		return time;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.hswt.riskident.si.dao.api.SIDataSearchService#
	 * searchByNameFirstCharacter (char)
	 */
	@Override
	public List<Entry> searchByNameFirstCharacter(char character) {

		CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
		CriteriaQuery<Entry> criteriaQuery = criteriaBuilder.createQuery(Entry.class);
		Root<Entry> entry = criteriaQuery.from(Entry.class);
		CriteriaQuery<Entry> query = criteriaQuery.select(entry);

		Join<Entry, AdditionalNameStringProperty> joinAdditionalNames = entry.joinSet("additionalNames", JoinType.LEFT);
		List<Predicate> predicates = new ArrayList<>();
		addAdditionalNamePredicates(character, criteriaBuilder, entry, joinAdditionalNames, predicates);

		return getResultList(query, predicates);
	}

	private void addAdditionalNamePredicates(char character, CriteriaBuilder criteriaBuilder, Root<Entry> entry,
											 Join<Entry, AdditionalNameStringProperty> joinAdditionalNames,
											 List<Predicate> predicates) {
		predicates.add(criteriaBuilder.or(criteriaBuilder.like(criteriaBuilder.upper(entry.get("name").get("value")),
						(character + "%").toUpperCase()),
				criteriaBuilder.like(criteriaBuilder.upper(joinAdditionalNames.get("value")),
						(character + "%").toUpperCase())));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.hswt.riskident.si.dao.api.SIDataSearchService#searchByAccurateMass
	 * (de.hswt.riskident.si.model.SINumberSearchProperty)
	 */
	@Override
	public List<Entry> searchByAccurateMass(NumberSearchProperty accurateMass) {
		if (accurateMass == null) {
			return Collections.emptyList();
		}

		double value = accurateMass.getValue();
		double range = SearchUtil.getRangeFromPPM(value, accurateMass.getRange());

		CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
		CriteriaQuery<Entry> criteriaQuery = criteriaBuilder.createQuery(Entry.class);

		Root<Entry> entry = criteriaQuery.from(Entry.class);
		CriteriaQuery<Entry> query = criteriaQuery.select(entry);

		List<Predicate> predicates = new ArrayList<>();

		predicates.add(
				criteriaBuilder.ge(entry.get("accurateMass").get("value"), value - range));
		predicates.add(
				criteriaBuilder.le(entry.get("accurateMass").get("value"), value + range));

		return getResultList(query, predicates);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.hswt.riskident.si.dao.api.SIDataSearchService#
	 * searchByAccurateMassAndPh
	 * (de.hswt.riskident.si.model.SINumberSearchProperty, double)
	 */
	@Override
	public List<Entry> searchByAccurateMassAndPh(NumberSearchProperty accurateMass, double ph) {
		if (accurateMass == null) {
			return Collections.emptyList();
		}

		double val = accurateMass.getValue();
		double range = SearchUtil.getRangeFromPPM(val, accurateMass.getRange());

		CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
		CriteriaQuery<Entry> criteriaQuery = criteriaBuilder.createQuery(Entry.class);
		Root<Entry> entry = criteriaQuery.from(Entry.class);

		CriteriaQuery<Entry> query = criteriaQuery.select(entry);

		List<Predicate> predicates = new ArrayList<>();

		predicates.add(criteriaBuilder.ge(entry.get("accurateMass").get("value"), val - range));
		predicates.add(criteriaBuilder.le(entry.get("accurateMass").get("value"), val + range));
		predicates.add(criteriaBuilder.equal(entry.get("logdValues").get("ph"), ph));

		criteriaQuery.where(
				criteriaBuilder.ge(entry.get("accurateMass").get("value"), val - range),
				criteriaBuilder.le(entry.get("accurateMass").get("value"), val + range),
				criteriaBuilder.equal(entry.get("logdValues").get("ph"), ph));

		return getResultList(query, predicates);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.hswt.riskident.si.dao.api.SIDataSearchService#searchDynamic(de.hswt
	 * .riskident.si.model.SISearchParameter)
	 */
	@Override
	public List<Entry> searchDynamic(SearchParameter searchParameter) {
		LOGGER.debug("enter searchDynamic with parameter {}", searchParameter);

		CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
		CriteriaQuery<Entry> criteriaQuery = criteriaBuilder.createQuery(Entry.class);

		// create root => interests and entity of inner type
		Root<Entry> entry = criteriaQuery.from(Entry.class);
		Join<Entry, LogPNumberProperty> joinLogPs = entry.joinSet("logpValues", JoinType.LEFT);

		// add the predicates
		CriteriaQuery<Entry> query = criteriaQuery.select(entry);

		List<Predicate> predicates = new ArrayList<>();

		if (!isNullOrEmpty(searchParameter.getName())) {
			addNameSearchPredicates(searchParameter.getName(), criteriaBuilder, entry, predicates);
		}
		if (!isNullOrEmpty(searchParameter.getCas())) {
			addCasSearchPredicates(searchParameter, criteriaBuilder, entry, predicates);
		}
		if (!isNullOrEmpty(searchParameter.getSmiles())) {
			addSMILESSearchPredicates(searchParameter, criteriaBuilder, entry, predicates);
		}
		if (!isNullOrEmpty(searchParameter.getIupac())) {
			addIupacSearchPredicates(searchParameter, criteriaBuilder, entry, predicates);
		}
		if (searchParameter.getAccurateMass() != null) {
			addAccurateMassSearchPredicates(searchParameter, criteriaBuilder, entry, predicates);
		} else if (massRangeIsValid(searchParameter)) {
			addMassRangeSearchPredicates(searchParameter, criteriaBuilder, entry, predicates);
		}
		if (!isNullOrEmpty(searchParameter.getElementalFormula())) {
			addElementalFormulaSearchPredicates(searchParameter, criteriaBuilder, entry, predicates);
		}
		if (searchParameter.getLogP() != null) {
			addAccurateLogPSearchPredicates(searchParameter, criteriaBuilder, joinLogPs, predicates);
		} else if (logPRangeIsValid(searchParameter)) {
			addLogPRangeSearchPredicates(searchParameter, criteriaBuilder, joinLogPs, predicates);
		}
		if (!searchParameter.getHalogens().isEmpty()) {
			addHalogensPredicates(searchParameter, criteriaBuilder, entry, predicates);
		}
		if (!isNullOrEmpty(searchParameter.getPublicID())) {
			addPublicIdSearchPredicates(searchParameter, criteriaBuilder, entry, predicates);
		}
		if (!isNullOrEmpty(searchParameter.getInchiKey())) {
			addInchiKeySearchPredicates(searchParameter, criteriaBuilder, entry, predicates);
		}

		return getResultList(query, predicates);
	}

	private List<Entry> getResultList(CriteriaQuery<Entry> query, List<Predicate> predicates) {
		if (!predicates.isEmpty()) {
			query.where(predicates.toArray(new Predicate[0]));
		}

		query.distinct(true);
		TypedQuery<Entry> allQuery = getEntityManager().createQuery(query);
		List<Entry> results = allQuery.getResultList();

		getEntityManager().close();

//		LOGGER.debug("leaving search, return {} results", results.size());
		return results;
	}

	private boolean logPRangeIsValid(SearchParameter searchParameter) {
		return Double.compare(searchParameter.getLogPRangeMin(), Double.MIN_VALUE) != 0
				&& Double.compare(searchParameter.getLogPRangeMax(), Double.MAX_VALUE) != 0;
	}

	private boolean massRangeIsValid(SearchParameter searchParameter) {
		return Double.compare(searchParameter.getAccurateMassRangeMin(), Double.MIN_VALUE) != 0
				&& Double.compare(searchParameter.getAccurateMassRangeMax(),
				Double.MAX_VALUE) != 0;
	}

	private void addInchiKeySearchPredicates(SearchParameter searchParameter, CriteriaBuilder criteriaBuilder,
											 Root<Entry> entry, List<Predicate> predicates) {
		LOGGER.debug("add predicate for Inchi Key  {}", searchParameter.getInchiKey());
		String inchiKey = searchParameter.getInchiKey();
		predicates.add(criteriaBuilder.like(entry.get("inchiKey").get("value"), "%" + inchiKey + "%"));
	}

	private void addPublicIdSearchPredicates(SearchParameter searchParameter, CriteriaBuilder criteriaBuilder,
											 Root<Entry> entry, List<Predicate> predicates) {
		LOGGER.debug("add predicate for Public ID {}", searchParameter.getPublicID());
		String publicID = searchParameter.getPublicID();
		predicates.add(criteriaBuilder.like(entry.get("publicID"), publicID));
	}

	private void addHalogensPredicates(SearchParameter searchParameter, CriteriaBuilder criteriaBuilder,
									   Root<Entry> entry, List<Predicate> predicates) {
		LOGGER.debug("add predicate for halogens");
		for (String halogen : searchParameter.getHalogens()) {
			predicates.add(criteriaBuilder.like(entry.get("elementalFormula").get("value"), "%" + halogen + "%"));
		}
	}

	private void addLogPRangeSearchPredicates(SearchParameter searchParameter, CriteriaBuilder criteriaBuilder,
											  Join<Entry, LogPNumberProperty> joinLogPs, List<Predicate> predicates) {
		LOGGER.debug("add predicate for logD range between {} and {}",
				searchParameter.getLogPRangeMin(), searchParameter.getLogPRangeMax());
		predicates.add(criteriaBuilder.between(joinLogPs.get("value"),
				searchParameter.getLogPRangeMin(), searchParameter.getLogPRangeMax()));
	}

	private void addAccurateLogPSearchPredicates(SearchParameter searchParameter, CriteriaBuilder criteriaBuilder,
												 Join<Entry, LogPNumberProperty> joinLogPs, List<Predicate> predicates) {
		double val = searchParameter.getLogP();
		double range = searchParameter.getLogPDelta();
		LOGGER.debug("add predicate for logP {}", val);
		predicates.add(criteriaBuilder.between(joinLogPs.get("value"), val - range,
				val + range));
	}

	private void addElementalFormulaSearchPredicates(SearchParameter searchParameter, CriteriaBuilder criteriaBuilder,
													 Root<Entry> entry, List<Predicate> predicates) {
		LOGGER.debug("add predicate for formula {}", searchParameter.getElementalFormula());
		String formula = searchParameter.getElementalFormula().replace(" ", "");
		predicates.add(criteriaBuilder.like(entry.get("elementalFormula").get("value"),
				"%" + formula + "%"));
	}

	private void addMassRangeSearchPredicates(SearchParameter searchParameter, CriteriaBuilder criteriaBuilder,
											  Root<Entry> entry, List<Predicate> predicates) {
		double ionisation = SearchUtil.getIonisation(searchParameter.getIonisation());
		LOGGER.debug("add predicate for mass range between {} and {}",
				searchParameter.getAccurateMassRangeMin() + ionisation,
				searchParameter.getAccurateMassRangeMax() + ionisation);
		predicates.add(criteriaBuilder.between(entry.get("accurateMass").get("value"),
				searchParameter.getAccurateMassRangeMin() + ionisation,
				searchParameter.getAccurateMassRangeMax() + ionisation));
	}

	private void addAccurateMassSearchPredicates(SearchParameter searchParameter, CriteriaBuilder criteriaBuilder,
												 Root<Entry> entry, List<Predicate> predicates) {
		double val = getMassSearchValue(searchParameter);
		double range = SearchUtil.getRangeFromPPM(val, searchParameter.getPpm());
		LOGGER.debug("add predicate for mass {}", val);
		predicates.add(criteriaBuilder.between(entry.get("accurateMass").get("value"), val - range, val + range));
	}

	private void addIupacSearchPredicates(SearchParameter searchParameter, CriteriaBuilder criteriaBuilder,
										  Root<Entry> entry, List<Predicate> predicates) {
		String iupac = searchParameter.getIupac();
		LOGGER.debug("add predicate for IUPAC {}", iupac);
		predicates.add(criteriaBuilder.like(entry.get("iupac").get("value"), "%" + iupac + "%"));
	}

	private void addCasSearchPredicates(SearchParameter searchParameter, CriteriaBuilder criteriaBuilder,
										Root<Entry> entry, List<Predicate> predicates) {
		String cas = searchParameter.getCas();
		LOGGER.debug("add predicate for CAS {}", cas);
		predicates.add(criteriaBuilder.equal(entry.get("cas").get("value"), cas));
	}

	private void addSMILESSearchPredicates(SearchParameter searchParameter, CriteriaBuilder criteriaBuilder,
										   Root<Entry> entry, List<Predicate> predicates) {
		LOGGER.debug("add predicate for SMILES {}", searchParameter.getSmiles());
		String smiles = searchParameter.getSmiles();
		Predicate smilesPredicate = criteriaBuilder.like(entry.get("smiles").get("value"), "%" + smiles + "%");
		predicates.add(smilesPredicate);
	}

	private void addNameSearchPredicates(String searchName, CriteriaBuilder criteriaBuilder, Root<Entry> entry,
										 List<Predicate> predicates) {
		LOGGER.debug("add predicate for name {}", searchName);
		Join<Entry, AdditionalNameStringProperty> joinAdditionalNames = entry
				.joinSet("additionalNames", JoinType.LEFT);
		// Case insensitive search predicates
		String name = searchName.toUpperCase();
		predicates.add(criteriaBuilder.or(
				criteriaBuilder.like(criteriaBuilder.upper(entry.get("name").get("value")), "%" + name + "%"),
				criteriaBuilder.like(criteriaBuilder.upper(joinAdditionalNames.get("value")),
						"%" + name + "%")));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.hswt.riskident.si.dao.api.SIDataSearchService#getAllCategories()
	 */
//	@Override
//	public List<CategoryStringProperty> getAllCategories() {
//		CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
//		CriteriaQuery<CategoryStringProperty> cq = cb.createQuery(CategoryStringProperty.class);
//		Root<CategoryStringProperty> rootEntry = cq.from(CategoryStringProperty.class);
//		cq.orderBy(cb.asc(rootEntry.get("value")));
//		CriteriaQuery<CategoryStringProperty> all = cq.select(rootEntry);
//		TypedQuery<CategoryStringProperty> allQuery = getEntityManager().createQuery(all);
//		List<CategoryStringProperty> results = allQuery.getResultList();
//		getEntityManager().close();
//		return results;
//	}

	@Override
	public Page<WebserviceEntry> findByFormula(String elementalFormula, Pageable pageable) {
		return getEntryRepository().findByFormula(elementalFormula, pageable);
	}

	@Override
	public Page<WebserviceEntry> findByAccurateMass(Double accurateMassMin, Double accurateMassMax, Pageable pageable) {
		return getEntryRepository().findByAccurateMass(accurateMassMin, accurateMassMax, pageable);
	}


	@Override
	public Page<String> findAllInchiKeys(Pageable pageable) {
		return getEntryRepository().findAllInchiKeys(pageable);
	}

	@Override
	public Page<Object> findAllInchiKeysAndPublicIDs(Pageable pageable) {
		return getEntryRepository().findAllInchiKeysAndStoffidentIds(pageable);
	}

	@Override
	public Page<String> findAllPublicIDs(Pageable pageable) {
		return getEntryRepository().findAllPublicIDs(pageable);
	}

	@Override
	public Optional<WebserviceEntry> findByPublicID(String publicID) {
		return getEntryRepository().findByPublicID(publicID);
	}

	@Override
	public Optional<WebserviceEntry> findByInchiKeyValue(String inchiKey) {
		return getEntryRepository().findByInchiKeyValue(inchiKey);
	}

	/**
	 * Gets the mass search value.
	 *
	 * @param searchParameter the search parameter
	 * @return the mass search value
	 */
	private double getMassSearchValue(SearchParameter searchParameter) {
		double mass = searchParameter.getAccurateMass();
		mass += SearchUtil.getIonisation(searchParameter.getIonisation());
		return mass;
	}

	@Override
    public void writeDatabase(Set<Entry> entries, Set<SourceList> sourceLists, Set<CategoryStringProperty> categories) {

	    // Create new transaction for database persistence
        LOGGER.debug("Starting transaction ...");
        EntityManager entityManager = getEntityManager().getEntityManagerFactory().createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        deleteAllCompounds(entityManager);
        writeSourceLists(sourceLists, entityManager);
        writeCategories(categories, entityManager);
        writeCompounds(entries, entityManager);

        transaction.commit();
        entityManager.close();

        LOGGER.debug("Transaction done!");
    }

    private void deleteAllCompounds(EntityManager entityManager) {
        LOGGER.debug("deleting all Compounds ...");
        entityManager.createQuery("DELETE FROM LogDNumberProperty").executeUpdate();
        entityManager.createQuery("DELETE FROM LogPNumberProperty").executeUpdate();
        entityManager.createQuery("DELETE FROM AdditionalNameStringProperty").executeUpdate();
        entityManager.createQuery("DELETE FROM MassBankIdStringProperty").executeUpdate();
        entityManager.createQuery("DELETE FROM CategoryStringProperty").executeUpdate();
        entityManager.createQuery("DELETE FROM SourceList").executeUpdate();
        entityManager.createQuery("DELETE FROM Entry").executeUpdate();
        entityManager.flush();
    }

	private void writeCategories(Set<CategoryStringProperty> categories, EntityManager entityManager) {
		LOGGER.debug("writing categories ...");
		categories.forEach(entityManager::persist);
        entityManager.flush();
	}

	private void writeSourceLists(Set<SourceList> sourceLists, EntityManager entityManager) {
		LOGGER.debug("writing source lists ...");
		sourceLists.forEach(entityManager::persist);
        entityManager.flush();
	}

	private void writeCompounds(Set<Entry> entries, EntityManager entityManager) {
		LOGGER.debug("writing entries ...");
		entries.forEach(entityManager::persist);
        entityManager.flush();
	}
}
