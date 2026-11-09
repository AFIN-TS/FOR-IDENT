package de.hswt.fi.fileimport.service.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.hswt.fi.model.FeatureSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

@Component
@Scope("prototype")
public class JsonReader {

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonReader.class);

	private ObjectMapper objectMapper;

	// Jackson 2 with the settings of the ObjectMapper of Spring Boot 2.7 that was injected before (SP-10: Spring Boot 4
	// only provides Jackson 3)
	@Autowired
	public JsonReader() {
		this(JsonMapper.builder()
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
				.build());
	}

	public JsonReader(ObjectMapper objectMapper) {
		this.objectMapper = Objects.requireNonNull(objectMapper);
		addDeserializer();
	}

	FeatureSet parseFile(Path path) {
		if (path == null) {
			throw new NullPointerException("Path must not be null");
		}

		try {
			return objectMapper.readValue(path.toFile(), FeatureSet.class);
		} catch (IOException e) {
			LOGGER.error("Can not parse given content as JSON {}", e);
		}
		return null;
	}

	FeatureSet parseString(String jsonString) {
		try {
			return objectMapper.readValue(jsonString, FeatureSet.class);
		} catch (IOException e) {
			LOGGER.error("An error occured while parsing string", e);
		}
		return null;
	}

	private void addDeserializer() {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(FeatureSet.class, new FeatureSetDeserializer());
		objectMapper.registerModule(module);
	}

	boolean isValidJson(Path path) {

		if(!path.toString().toLowerCase().endsWith(".json")) {
			return false;
		}

		try {
			final ObjectMapper mapper = new ObjectMapper();
			mapper.readTree(path.toFile());
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	boolean isValidJson(String json) {

		try {
			final ObjectMapper mapper = new ObjectMapper();
			mapper.readTree(json);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

}
