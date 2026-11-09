package de.hswt.fi.search.service.search.stoffident;

import de.hswt.fi.common.spring.Profiles;
import de.hswt.fi.search.service.search.api.AbstractCompoundService;
import de.hswt.fi.search.service.search.api.repositories.EntryRepository;
import de.hswt.fi.search.service.search.stoffident.config.StoffIdentDatabaseConfiguration;
import de.hswt.fi.search.service.search.stoffident.repositories.StoffidentEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Profile({Profiles.LC, Profiles.DEVELOPMENT_LC, Profiles.TEST})
@Primary
@Service
@Transactional(transactionManager = StoffIdentDatabaseConfiguration.TRANSACTION_MANAGER, readOnly = true)
public class StoffidentCompoundService extends AbstractCompoundService {

	private static final Logger LOGGER = LoggerFactory.getLogger(StoffidentCompoundService.class);
	private final StoffidentEntryRepository entryRepository;

	@PersistenceContext(unitName = StoffIdentDatabaseConfiguration.ENTITY_MANAGER)
	private EntityManager entityManager;

	@Autowired
	public StoffidentCompoundService(StoffidentEntryRepository entryRepository) {
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
		return StoffIdentDatabaseConfiguration.ID_PREFIX;
	}

	@Override
	public String getDatasourceName() {
		return StoffIdentDatabaseConfiguration.DATABASE_NAME;
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
