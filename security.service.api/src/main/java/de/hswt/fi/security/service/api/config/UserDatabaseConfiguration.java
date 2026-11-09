package de.hswt.fi.security.service.api.config;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import static de.hswt.fi.security.service.api.config.UserDatabaseConfiguration.*;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
		basePackages = {BASE_PACKAGE, BASE_PACKAGE_2},
		// Explicit filter: with Spring Data MongoDB on the classpath, Spring Data 4 otherwise skips these repositories
		// in its strict multi-store check (SP-10)
		includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
				classes = org.springframework.data.repository.Repository.class),
		entityManagerFactoryRef = ENTITY_MANAGER,
		transactionManagerRef = TRANSACTION_MANAGER
)
public class UserDatabaseConfiguration {

	private static final String CONFIGURATION_PREFIX = "spring.datasource.user";

	static final String BASE_PACKAGE = "de.hswt.fi.security.service.api.repositories";

	static final String BASE_PACKAGE_2 = "de.hswt.fi.userproperties.service.api.repository";

	public static final String ENTITY_MANAGER = "userEntityManager";

	public static final String TRANSACTION_MANAGER = "userTransactionManager";

	private static final String PERSISTENCE_UNIT = "userPersistenceUnit";

	private static final String[] ENTITY_PACKAGES = new String[] {
			"de.hswt.fi.security.service.model",
			"de.hswt.fi.userproperties.service.model"};

	private static final String DATA_SOURCE = "userDataSource";

	@Primary
	@Bean
	@ConfigurationProperties(CONFIGURATION_PREFIX)
	public DataSourceProperties dataSourceProperties() {
		return new DataSourceProperties();
	}

	@Primary
	@Bean(name = DATA_SOURCE)
	public DataSource userDataSource() {
		return dataSourceProperties().initializeDataSourceBuilder().build();
	}

	@Primary
	@Bean(name = ENTITY_MANAGER)
	public LocalContainerEntityManagerFactoryBean userEntityManager(@Qualifier(DATA_SOURCE) DataSource dataSource,
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

	@Primary
	@Bean(name = TRANSACTION_MANAGER)
	public PlatformTransactionManager userTransactionManager(@Qualifier(ENTITY_MANAGER) EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}
}
