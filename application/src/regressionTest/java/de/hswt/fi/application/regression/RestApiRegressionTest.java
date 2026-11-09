package de.hswt.fi.application.regression;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls the public REST API (/api/substances, no login) of the running application and compares
 * status and body with the golden files in golden/api. The order of the result lists is not
 * guaranteed by the database, so list entries are sorted before comparison.
 */
@AutoConfigureTestRestTemplate
public class RestApiRegressionTest extends AbstractRegressionTest {

	private static final Map<String, String> QUERIES = new LinkedHashMap<>();

	static {
		QUERIES.put("by-inchikey-phenazone", "/api/substances?inchiKey=VEQOALNAAJBPNY-UHFFFAOYSA-N");
		QUERIES.put("by-formula-chlorpyrifos", "/api/substances?elementalFormula=C9H11Cl3NO3PS");
		QUERIES.put("by-mass-range-200", "/api/substances?accurateMassMin=200&accurateMassMax=200.05");
		QUERIES.put("by-mass-range-too-large", "/api/substances?accurateMassMin=100&accurateMassMax=400");
		QUERIES.put("by-ids", "/api/substances?id=SI00000001&id=SI00000002&id=PI00000001&id=PI00000002&id=DI00000001");
		QUERIES.put("by-unknown-inchikey", "/api/substances?inchiKey=AAAAAAAAAAAAAA-UHFFFAOYSA-N");
		QUERIES.put("inchikeys-page", "/api/substances/inchiKeys?page=0&size=20");
		QUERIES.put("ids-page", "/api/substances/ids?page=0&size=20");
		QUERIES.put("inchikeys-and-ids-page", "/api/substances/inchiKeysAndIds?page=0&size=20");
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Autowired
	private TestRestTemplate restTemplate;

	// Optional base URL of a production instance (e.g. https://for-ident.example.org): its responses must be
	// identical to the local ones (read-only GET requests of the public API)
	private static final String PRODUCTION_URL = System.getProperty("regression.productionUrl", "");

	@Test
	public void restApiResponsesMatchGoldenFiles() throws Exception {
		for (Map.Entry<String, String> query : QUERIES.entrySet()) {
			String local = describe(query.getValue(), get(restTemplate, query.getValue()));
			checkGolden("api/" + query.getKey() + ".json", local);
			if (!PRODUCTION_URL.isEmpty()) {
				String production = describe(query.getValue(),
						get(new TestRestTemplate(), PRODUCTION_URL + query.getValue()));
				checkEqual("production/" + query.getKey() + ".json", local, production);
			}
		}
	}

	// The API also answers in XML (jackson-dataformat-xml is on the classpath), so ask for JSON explicitly
	private static ResponseEntity<String> get(TestRestTemplate template, String url) {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		return template.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
	}

	private static String describe(String query, ResponseEntity<String> response) throws Exception {
		return "GET " + query + "\n" + "status " + response.getStatusCode().value() + "\n" + normalize(response.getBody());
	}

	private static String normalize(String body) throws Exception {
		if (body == null || body.isEmpty()) {
			return "(empty body)\n";
		}
		JsonNode json;
		try {
			json = MAPPER.readTree(body);
		} catch (Exception e) {
			return body + "\n";
		}
		removeTimestamp(json);
		sortArrays(json);
		// The property order of serialized Page objects differs between JVM starts, so sort the keys
		Object sortedKeys = MAPPER.treeToValue(json, Object.class);
		return MAPPER.writer(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).withDefaultPrettyPrinter()
				.writeValueAsString(sortedKeys) + "\n";
	}

	private static void removeTimestamp(JsonNode json) {
		if (json.isObject() && json.has("timestamp")) {
			((com.fasterxml.jackson.databind.node.ObjectNode) json).remove("timestamp");
		}
	}

	private static void sortArrays(JsonNode node) {
		if (node.isObject()) {
			node.fields().forEachRemaining(field -> sortArrays(field.getValue()));
		} else if (node.isArray()) {
			ArrayNode array = (ArrayNode) node;
			List<JsonNode> elements = new ArrayList<>();
			array.forEach(element -> {
				sortArrays(element);
				elements.add(element);
			});
			elements.sort(Comparator.comparing(JsonNode::toString));
			array.removeAll();
			elements.forEach(array::add);
		}
	}
}
