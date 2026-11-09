package de.hswt.fi.web.service.def.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Description of the REST API (SP-14): springdoc generates it from the controllers, so it cannot drift from the code.
 * OpenAPI document at {@code /api/openapi}, Swagger UI at {@code /api/docs} (see application.properties).
 */
@Configuration
public class OpenApiConfiguration {

	static final String SEARCH_PATH = "/api/substances";

	@Bean
	public OpenAPI apiDescription() {
		String version = OpenApiConfiguration.class.getPackage().getImplementationVersion();
		return new OpenAPI().info(new Info()
				.title("FOR-IDENT REST API")
				.description("Read-only access to the substances of the public databases. Results of the list "
						+ "endpoints are paged per database (page, size; default size 100); the pages of all databases "
						+ "are combined in one response.")
				.version(version == null ? "development" : version)
				.license(new License().name("LGPL-2.1").url("https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")));
	}

	/**
	 * The four searches of {@code GET /api/substances} differ only in their query parameters. OpenAPI allows one
	 * operation per path and method, so springdoc merges them, with the summary of one of them and all parameters
	 * required. This describes the merged operation as what it is: one search with alternative parameter sets.
	 */
	@Bean
	public OpenApiCustomizer substanceSearchDescription() {
		return openApi -> {
			PathItem path = openApi.getPaths() == null ? null : openApi.getPaths().get(SEARCH_PATH);
			Operation search = path == null ? null : path.getGet();
			if (search == null) {
				return;
			}
			search.setOperationId("searchSubstances");
			search.setSummary("Search substances");
			search.setDescription("""
					Give exactly one of these parameter sets:

					* `inchiKey` – the substance with this InChI key (one entry per database that contains it)
					* `id` – substances by ID, e.g. `id=SI00000528&id=PI00000001`; at most 100 IDs, IDs without \
					a substance are left out (the response is a list, not a page)
					* `elementalFormula` – substances whose formula contains this text, e.g. C11H12N2O also finds \
					C11H12N2O2 (paged)
					* `accurateMassMin` and `accurateMassMax` – substances in this monoisotopic mass range, at most \
					200 Da wide (paged)

					Errors are answered without a body: a mass range wider than 200 Da or more than 100 IDs with \
					400, a request without one of these parameter sets with 401 (as in all earlier versions).""");
			// No pre-filled values: the Swagger UI would otherwise send all parameter sets at once
			search.getParameters().forEach(parameter -> {
				parameter.setRequired(false);
				parameter.setExample(null);
				if (parameter.getSchema() != null) {
					parameter.getSchema().setExample(null);
				}
			});
		};
	}
}
