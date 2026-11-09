package de.hswt.fi.application;

import de.hswt.fi.common.spring.Profiles;
import de.hswt.fi.search.service.search.api.CompoundSearchService;
import de.hswt.fi.security.service.api.config.UserDatabaseConfiguration;
import de.hswt.fi.security.service.api.repositories.DatabaseRepository;
import de.hswt.fi.security.service.model.Database;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;

@Service
@Profile({Profiles.INITIALIZATION_LC, Profiles.INITIALIZATION_GC, Profiles.TEST})
@Transactional(transactionManager = UserDatabaseConfiguration.TRANSACTION_MANAGER)
public class UpdateAvailableDatabases {

	private final DatabaseRepository databaseRepository;

	private final List<CompoundSearchService> compoundSearchServices;

	@PersistenceContext(unitName = UserDatabaseConfiguration.ENTITY_MANAGER)
	private EntityManager entityManager;

	@Autowired
	public UpdateAvailableDatabases(DatabaseRepository databaseRepository, List<CompoundSearchService> compoundSearchServices) {
		this.databaseRepository = databaseRepository;
		this.compoundSearchServices = compoundSearchServices;
	}

	@EventListener
	public void appReady(ApplicationReadyEvent event) {

		List<Database> newDatabases = new ArrayList<>();
		List<Database> removedDatabases = new ArrayList<>();

		List<Database> persistedDatabases = databaseRepository.findAll();

		for (CompoundSearchService compoundSearchService : compoundSearchServices) {
			if (persistedDatabases.stream().noneMatch(persistedDb -> persistedDb.getName().equals(compoundSearchService.getDatasourceName()))) {
				newDatabases.add(new Database(compoundSearchService.getDatasourceName(), compoundSearchService.isAccessible()));
			}
		}

		for (Database persistedDatabase : persistedDatabases) {
			if (compoundSearchServices.stream().noneMatch(configDb -> configDb.getDatasourceName().equals(persistedDatabase.getName()))) {
				removedDatabases.add(new Database(persistedDatabase.getName(), false));
			}
		}

		newDatabases.forEach(entityManager::persist);
		removedDatabases.forEach(entityManager::remove);
	}

}
