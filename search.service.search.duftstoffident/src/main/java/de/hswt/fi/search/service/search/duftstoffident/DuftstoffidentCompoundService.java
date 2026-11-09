package de.hswt.fi.search.service.search.duftstoffident;

import de.hswt.fi.common.spring.Profiles;
import de.hswt.fi.search.service.search.api.AbstractCompoundService;
import de.hswt.fi.search.service.search.api.repositories.EntryRepository;
import de.hswt.fi.search.service.search.duftstoffident.config.DuftStoffIdentDatabaseConfiguration;
import de.hswt.fi.search.service.search.duftstoffident.repositories.DuftstoffidentEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Profile({Profiles.GC, Profiles.DEVELOPMENT_GC})
@Primary
@Service
@Transactional(transactionManager = DuftStoffIdentDatabaseConfiguration.TRANSACTION_MANAGER, readOnly = true)
public class DuftstoffidentCompoundService extends AbstractCompoundService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DuftstoffidentCompoundService.class);

	private final DuftstoffidentEntryRepository entryRepository;

	@PersistenceContext(unitName = DuftStoffIdentDatabaseConfiguration.ENTITY_MANAGER)
	private EntityManager entityManager;

	@Autowired
	public DuftstoffidentCompoundService(DuftstoffidentEntryRepository entryRepository) {
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
		return DuftStoffIdentDatabaseConfiguration.ID_PREFIX;
	}

	@Override
	public String getDatasourceName() {
		return DuftStoffIdentDatabaseConfiguration.DATABASE_NAME;
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
