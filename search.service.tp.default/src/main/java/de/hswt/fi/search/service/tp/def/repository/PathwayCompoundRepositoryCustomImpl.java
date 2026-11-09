package de.hswt.fi.search.service.tp.def.repository;

import de.hswt.fi.search.service.tp.model.MatchingPathway;
import de.hswt.fi.search.service.tp.model.Pathway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.List;

import static de.hswt.fi.search.service.tp.def.config.PathwayDatabaseConfiguration.MONGO_TEMPLATE;

@Component
// Needs the pathway database (SP-16)
@ConditionalOnProperty(name = "spring.datasource.pathway.enabled", havingValue = "true")
public class PathwayCompoundRepositoryCustomImpl implements PathwayCompoundRepositoryCustom {

	private final MongoTemplate mongoTemplate;

	private static final String ID_FIELD = "id";

	private static final String COMPOUNDS_PATH = "compounds";

	private static final String NEUTRAL_MASS_PATH = "compounds.neutralMass";

	private static final String INCHI_KEY_PATH = "compounds.inChiKey";

	private static final String INCHI_KEYS_FIELD = "inChiKeys";

	@Autowired
	public PathwayCompoundRepositoryCustomImpl(@Qualifier(MONGO_TEMPLATE) MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public List<MatchingPathway> aggregateCompoundsInMassRange(double lowerMass, double upperMass) {

		MatchOperation match = Aggregation.match(Criteria.where(NEUTRAL_MASS_PATH).gt(lowerMass).lt(upperMass));
		UnwindOperation unwind = Aggregation.unwind(COMPOUNDS_PATH);
		GroupOperation group = Aggregation.group(ID_FIELD).push(INCHI_KEY_PATH).as(INCHI_KEYS_FIELD);

		return mongoTemplate.aggregate(Aggregation.newAggregation(match, unwind, match, group), Pathway.class, MatchingPathway.class)
				.getMappedResults();
	}
}
