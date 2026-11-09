package de.hswt.fi.search.service.search.plantident;

import de.hswt.fi.common.spring.Profiles;
import de.hswt.fi.search.service.search.api.AbstractCompoundService;
import de.hswt.fi.search.service.search.api.repositories.EntryRepository;
import de.hswt.fi.search.service.search.plantident.config.PlantIdentDatabaseConfiguration;
import de.hswt.fi.search.service.search.plantident.repositories.PlantidentEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Profile({Profiles.LC, Profiles.DEVELOPMENT_LC, Profiles.TEST})
@Service
@Transactional(transactionManager = PlantIdentDatabaseConfiguration.TRANSACTION_MANAGER, readOnly = true)
public class PlantidentCompoundService extends AbstractCompoundService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PlantidentCompoundService.class);
	private final PlantidentEntryRepository entryRepository;

	@PersistenceContext(unitName = PlantIdentDatabaseConfiguration.ENTITY_MANAGER)
	private EntityManager entityManager;

	@Autowired
	public PlantidentCompoundService(PlantidentEntryRepository entryRepository) {
		this.entryRepository = entryRepository;
	}

	@Override
	public EntryRepository getEntryRepository() {
		return entryRepository;
	}

	@Override
	public EntityManager getEntityManager() {
		return entityManager;
	}

	@Override
	public String getIdPrefix() {
		return PlantIdentDatabaseConfiguration.ID_PREFIX;
	}

	@Override
	public String getDatasourceName() {
		return PlantIdentDatabaseConfiguration.DATABASE_NAME;
	}

	@Override
	public boolean isAccessible() {
		return true;
	}

	@Override
	public int getIndex() {
		return 0;
	}
}
