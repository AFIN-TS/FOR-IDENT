package de.hswt.fi.application;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Schema migrations of the PostgreSQL databases with Flyway (SP-16), one Flyway instance per database: the
 * databases are configured per module, which the Spring Boot auto-configuration does not support.
 *
 * <p>Version 1 is the schema of FOR-IDENT 2.x ({@code db/migration/<users|substances>/V1__baseline.sql}). An existing
 * database without Flyway history is recorded as version 1 without running the script (baseline); an empty database
 * gets its tables from the script. Later schema changes are added as V2, V3, ... All entity managers wait for the
 * migrations, so the tables exist before the application uses them (e.g. the initialization profile creating the
 * admin).
 *
 * <p>Switched off with {@code de.hswt.fi.flyway.enabled=false} (test profile: in-memory H2 databases whose tables
 * EclipseLink generates).
 */
@Configuration
@ConditionalOnProperty(name = "de.hswt.fi.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseMigrationConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMigrationConfiguration.class);

	static final String MIGRATIONS_BEAN = "databaseMigrations";

	/** Data source beans and the folder of their migration scripts; only the beans of the active profile exist. */
	static final Map<String, String> LOCATIONS = new LinkedHashMap<>();

	static {
		LOCATIONS.put("userDataSource", "db/migration/users");
		LOCATIONS.put("stoffidentDataSource", "db/migration/substances");
		LOCATIONS.put("plantidentDataSource", "db/migration/substances");
		// GC instance (profile gc): DuftSTOFF-IDENT has the same schema as the LC substance databases
		LOCATIONS.put("duftstoffidentDataSource", "db/migration/substances");
	}

	@Bean(name = MIGRATIONS_BEAN)
	public InitializingBean databaseMigrations(BeanFactory beanFactory) {
		return () -> LOCATIONS.forEach((dataSourceName, location) -> {
			if (beanFactory.containsBean(dataSourceName)) {
				migrate(dataSourceName, beanFactory.getBean(dataSourceName, DataSource.class), location);
			}
		});
	}

	static MigrateResult migrate(String name, DataSource dataSource, String location) {
		Flyway flyway = Flyway.configure()
				.dataSource(dataSource)
				.locations("classpath:" + location)
				.baselineOnMigrate(true)
				.baselineVersion("1")
				.baselineDescription("schema of FOR-IDENT 2.x")
				.load();
		MigrateResult result = flyway.migrate();
		MigrationInfo current = flyway.info().current();
		LOGGER.info("database {}: schema version {} ({} migrations applied)", name,
				current == null ? "-" : current.getVersion(), result.migrationsExecuted);
		return result;
	}

	/** The entity managers are created after the migrations. */
	@Bean
	public static BeanFactoryPostProcessor entityManagersAfterMigrations() {
		return beanFactory -> {
			Set<String> names = new LinkedHashSet<>();
			names.addAll(Arrays.asList(beanFactory.getBeanNamesForType(EntityManagerFactory.class, true, false)));
			names.addAll(Arrays.asList(beanFactory.getBeanNamesForType(AbstractEntityManagerFactoryBean.class, true, false)));
			for (String name : names) {
				String beanName = name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX) ? name.substring(1) : name;
				addDependsOn(beanFactory, beanName);
			}
		};
	}

	private static void addDependsOn(ConfigurableListableBeanFactory beanFactory, String beanName) {
		if (!beanFactory.containsBeanDefinition(beanName)) {
			return;
		}
		BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
		Set<String> dependsOn = new LinkedHashSet<>();
		if (definition.getDependsOn() != null) {
			dependsOn.addAll(Arrays.asList(definition.getDependsOn()));
		}
		dependsOn.add(MIGRATIONS_BEAN);
		definition.setDependsOn(dependsOn.toArray(String[]::new));
	}
}
