package de.hswt.fi.database.importer.tp.def;

import de.hswt.fi.database.importer.tp.api.TransformationProductDataProvider;
import de.hswt.fi.database.importer.tp.api.TransformationProductImportService;
import de.hswt.fi.search.service.tp.def.config.PathwayDatabaseConfiguration;
import de.hswt.fi.search.service.tp.model.Compound;
import de.hswt.fi.search.service.tp.model.Pathway;
import de.hswt.fi.search.service.tp.model.Transformation;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

@Service
@SuppressWarnings("unused")
// Needs the pathway database (SP-16)
@ConditionalOnProperty(name = "spring.datasource.pathway.enabled", havingValue = "true")
public class DefaultTransformationPathImporterService
		implements TransformationProductImportService {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DefaultTransformationPathImporterService.class);

	private List<TransformationProductDataProvider> dataProviders;

	private MongoTemplate mongoTemplate;

	@Autowired
	public DefaultTransformationPathImporterService(List<TransformationProductDataProvider> dataProviders,
													@Qualifier(PathwayDatabaseConfiguration.MONGO_TEMPLATE) MongoTemplate mongoTemplate) {
		this.dataProviders = dataProviders;
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public void createData() {
		Map<TransformationProductDataProvider, List<Pathway>> pathwaysMap = null;

		LOGGER.info("begin to read transformation product pathways");

		try {
			pathwaysMap = getPathways();
		} catch (ImportServiceException e) {
			LOGGER.error(
					"problems while reading transformation product pathways with data provider {}",
					e.getDataProvider());
			// TODO It seems that their must be an error or change
			// so that no pathways could be read.
			// If the import runs as chron, an error must be send
			// to an admin, which can have a further look at data provider.
		}

		if (pathwaysMap == null) {
			LOGGER.error("did not update the transformation product pathways");
			return;
		}

		// backup old pathways
		// TODO when this runs every week, the disk space will not last long
		// so remove the old ones, maybe let the 2 or 3 last versions left
		//String backupName = Long.toString(System.currentTimeMillis());
		//mongoTemplate.getCollection(mongoTemplate.getCollectionName(Pathway.class)).rename(backupName);

		// store new gathered pathways to db
		for (List<Pathway> pathways : pathwaysMap.values()) {
			for (Pathway pathway : pathways) {
				mongoTemplate.insert(pathway);
			}
		}

		LOGGER.info("updated transformation product pathways");

		final Map<TransformationProductDataProvider, List<Pathway>> map = pathwaysMap;
		pathwaysMap.keySet()
				.forEach(k -> LOGGER.debug("updated tp's pathways from {} with {} pathways",
						map.get(k).size(), k.getClass()));

		// TODO send a mail to admins, when succeed, include the size of the
		// lists of the data providers inculding the provider class names
	}

	/**
	 * Reads pathways from all available data providers.
	 *
	 * @return a map with all gathered pathways, with the data provider as key
	 * @throws ImportServiceException
	 *             when a data provider seems to return a null or emtpy result
	 */
	private Map<TransformationProductDataProvider, List<Pathway>> getPathways()
			throws ImportServiceException {
		Map<TransformationProductDataProvider, List<Pathway>> pathwaysMap = new HashMap<>();

		for (TransformationProductDataProvider dataProvider : dataProviders) {
			LOGGER.debug("start getting transformation product pathways from provider {}",
					dataProvider.getClass());
			List<Pathway> pathways = dataProvider.getPathways();
			if (pathways == null || pathways.isEmpty()) {
				throw new ImportServiceException(dataProvider.getClass());
			}
			for (Pathway pathway : pathways) {
				preparePathway(pathway);
			}
			pathwaysMap.put(dataProvider, pathways);
			LOGGER.debug("finished getting transformation product pathways from provider {}",
					dataProvider.getClass());
		}

		return pathwaysMap;
	}

	/**
	 * Traverse pathway and save each compound and transformation in sets. Each
	 * compound and transformation gets a unique id.
	 *
	 * @param pathway
	 *            pathway to traverse
	 */
	private void preparePathway(Pathway pathway) {
		Stack<Compound> s = new Stack<>();
		s.push(pathway.getRoot());

		while (!s.isEmpty()) {
			Compound currentCompound = s.pop();
			if (!pathway.getCompounds().contains(currentCompound)) {
				currentCompound.setId(new ObjectId().toHexString());
				pathway.getCompounds().add(currentCompound);
				for (Transformation transformation : currentCompound.getTransformations()) {
					if (!pathway.getTransformations().contains(transformation)) {
						transformation.setId(new ObjectId().toHexString());
						pathway.getTransformations().add(transformation);
					}
					Compound compoundToTraverse = transformation.getTransformationProduct();
					if (pathway.getCompounds().contains(compoundToTraverse)
							|| s.contains(compoundToTraverse)) {
						continue;
					}
					s.push(compoundToTraverse);
				}
			}
		}

		updatePathwayIds(pathway);
	}

	private void updatePathwayIds(Pathway pathway) {
		pathway.setRootId(pathway.getRoot().getId());
		pathway.getCompounds().forEach(compound -> compound.getTransformations()
				.forEach(transformation -> compound.addTransformationId(transformation.getId())));
		pathway.getTransformations().forEach(transformation -> {
			transformation.setCompoundId(transformation.getCompound().getId());
			transformation
					.setTransformationProductId(transformation.getTransformationProduct().getId());
		});

	}

	private class ImportServiceException extends Exception {

		private static final long serialVersionUID = 1L;

		private final Class<? extends TransformationProductDataProvider> dataProvider;

		ImportServiceException(
				Class<? extends TransformationProductDataProvider> dataProvider) {
			this.dataProvider = dataProvider;
		}

		Class<? extends TransformationProductDataProvider> getDataProvider() {
			return dataProvider;
		}
	}
}
