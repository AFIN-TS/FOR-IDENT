package de.hswt.fi.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication(scanBasePackages = Application.BASE_PACKAGE)
@EntityScan(basePackages = Application.BASE_PACKAGE)
public class Application extends SpringBootServletInitializer {

	static final String BASE_PACKAGE = "de.hswt.fi";

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Application.class);
	}

	public static void main(String[] args) {
		// FreeMarker (mail templates) would pick Log4j 2 because its API is on the classpath (for CDK and MetFrag),
		// but the logging backend is Logback (SP-10)
		if (System.getProperty("org.freemarker.loggerLibrary") == null) {
			System.setProperty("org.freemarker.loggerLibrary", "SLF4J");
		}
		SpringApplication.run(Application.class, args);
	}
}
