package de.hswt.fi.search.service.search.duftstoffident.config;

import de.hswt.fi.common.spring.Profiles;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import static de.hswt.fi.search.service.search.duftstoffident.config.DuftStoffIdentDatabaseConfiguration.*;

@Profile({Profiles.GC, Profiles.DEVELOPMENT_GC})
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
		basePackages = BASE_PACKAGE,
		// Explicit filter: with Spring Data MongoDB on the classpath, Spring Data 4 otherwise skips these repositories
		// in its strict multi-store check (SP-10)
		includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
				classes = org.springframework.data.repository.Repository.class),
		entityManagerFactoryRef = ENTITY_MANAGER,
		transactionManagerRef = TRANSACTION_MANAGER
)
public class DuftStoffIdentDatabaseConfiguration {

	public static final String ID_PREFIX = "DI";
	public static final String DATABASE_NAME = "DuftSTOFF-IDENT";
	public static final String ENTITY_MANAGER = "duftstoffidentEntityManager";
	public static final String TRANSACTION_MANAGER = "duftstoffidentTransactionManager";
	static final String BASE_PACKAGE = "de.hswt.fi.search.service.search.duftstoffident.repositories";
	private static final String CONFIGURATION_PREFIX = "spring.datasource.duftstoffident";
	private static final String PERSISTENCE_UNIT = "duftstoffidentPersistenceUnit";
	private static final String ENTITY_PACKAGES = "de.hswt.fi.search.service.mass.search.model";
	private static final String DATA_SOURCE = "duftstoffidentDataSource";
	private static final String DATA_SOURCE_PROPERTIES = "duftstoffidentDataSourceProperties";

	@Bean(name = DATA_SOURCE_PROPERTIES)
	@ConfigurationProperties(CONFIGURATION_PREFIX)
	public DataSourceProperties dataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean(name = DATA_SOURCE)
	public DataSource duftstoffidentDataSource() {
		return dataSourceProperties().initializeDataSourceBuilder().build();
	}

	@Bean(name = ENTITY_MANAGER)
	public LocalContainerEntityManagerFactoryBean duftstoffidentEntityManager(@Qualifier(DATA_SOURCE) DataSource dataSource,
																			  // Fix for generate-ddl property not picked up by spring with multiple data sources
																			  @Value("${" + CONFIGURATION_PREFIX + ".generate-ddl:false}") boolean generateDdl) {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource);
		em.setPackagesToScan(ENTITY_PACKAGES);

        EclipseLinkJpaVendorAdapter vendorAdapter = new EclipseLinkJpaVendorAdapter();
		vendorAdapter.setGenerateDdl(generateDdl);

		em.setJpaVendorAdapter(vendorAdapter);
		em.setPersistenceUnitName(PERSISTENCE_UNIT);
		// The entities are woven at build time (gradle/eclipselink-weaving.gradle), no -javaagent needed
		em.getJpaPropertyMap().put(PersistenceUnitProperties.WEAVING, "static");

		return em;
	}

	@Bean(name = TRANSACTION_MANAGER)
	public PlatformTransactionManager duftstoffidentransactionManager(@Qualifier(ENTITY_MANAGER) EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}
}
