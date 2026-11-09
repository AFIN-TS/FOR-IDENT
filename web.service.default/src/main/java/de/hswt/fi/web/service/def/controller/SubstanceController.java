package de.hswt.fi.web.service.def.controller;

import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.WebserviceEntry;
import de.hswt.fi.search.service.search.api.CompoundSearchService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping(path = SubstanceController.PATH)
@Tag(name = "Substances", description = "Substances of all public databases (STOFF-IDENT, PLANT-IDENT, ...)")
@SuppressWarnings("unused")
public class SubstanceController {

	private static final int DELTA_MASS_LIMIT = 200;

	private static final int PARAMETER_ID_LIMIT = 100;

	private static final Logger LOGGER = LoggerFactory.getLogger(SubstanceController.class);

	static final String PATH = "/api/substances";

	private List<CompoundSearchService> compoundSearchServices;

	@Autowired
	public SubstanceController(List<CompoundSearchService> compoundSearchServices) {
		this.compoundSearchServices = Objects.requireNonNull(compoundSearchServices, "StoffIdentSearchService must not be null");
	}

	private <T> Page<T> combinePages(Stream<Page<T>> pages) {
		List<T> entityStream = pages.map(Page::stream)
				.flatMap(Function.identity())
				.collect(Collectors.toList());

		return new UnpagedPage<>(entityStream);
	}

	// Listing all substances is not supported (only the identifiers below)
	@Hidden
	@GetMapping
	public Iterable<Entry> findAll() {
		throw new UnsupportedOperationException();
	}

	@Operation(summary = "All InChI keys", description = "InChI keys of all substances. The page and size parameters "
			+ "apply to each database; the result combines the pages of all databases.")
	@GetMapping(path = "/inchiKeys")
	public Page<String> findAllInchiKeys(@ParameterObject Pageable pageable) {
		LOGGER.debug("entering method findAllInchiKeys");
		return combinePages(compoundSearchServices.stream()
				.map(compoundSearchService -> compoundSearchService.findAllInchiKeys(pageable)));
	}

	@Operation(summary = "All substance IDs", description = "IDs of all substances (e.g. SI00000001 for STOFF-IDENT, "
			+ "PI... for PLANT-IDENT). The page and size parameters apply to each database.")
	@GetMapping(path = "/ids")
	public Page<String> findAllPublicIds(@ParameterObject Pageable pageable) {
		LOGGER.debug("entering method findAllPublicIDs");
		return combinePages(compoundSearchServices.stream()
				.map(compoundSearchService -> compoundSearchService.findAllPublicIDs(pageable)));
	}

	@Operation(summary = "All InChI keys with their substance IDs", description = "Pairs of InChI key and substance ID "
			+ "of all substances. The page and size parameters apply to each database.")
	@GetMapping(path = "/inchiKeysAndIds")
	public Page<Object> findAllInchiKeysAndPublicIds(@ParameterObject Pageable pageable) {
		LOGGER.debug("entering method findAllPublicIds");
		return combinePages(compoundSearchServices.stream()
				.map(compoundSearchService -> compoundSearchService.findAllInchiKeysAndPublicIDs(pageable)));
	}

	@Operation(summary = "Substances by elemental formula", description = "Finds formulas that contain the given text, "
			+ "e.g. C11H12N2O also finds C11H12N2O2.")
	@GetMapping(params = {"elementalFormula"})
	public Page<WebserviceEntry> findByElementalFormula(
			@Parameter(description = "Elemental formula, e.g. C16H12Cl2N4O4", example = "C6H5N3")
			@RequestParam String elementalFormula, @ParameterObject Pageable pageable) {
		LOGGER.debug("entering method findByElementalFormula");
		return combinePages(compoundSearchServices.stream()
				.map(compoundSearchService -> compoundSearchService.findByFormula(elementalFormula, pageable)));
	}

	@Operation(summary = "Substances by monoisotopic mass range", description = "The range may be at most "
			+ DELTA_MASS_LIMIT + " Da wide.")
	@GetMapping(params = {"accurateMassMin", "accurateMassMax"})
	public Page<WebserviceEntry> findByMass(
			@Parameter(description = "Lower limit of the monoisotopic mass (Da)", example = "119.04")
			@RequestParam Double accurateMassMin,
			@Parameter(description = "Upper limit of the monoisotopic mass (Da)", example = "119.06")
			@RequestParam Double accurateMassMax,
			@ParameterObject Pageable pageable) {

		LOGGER.debug("entering method findByMass");
		if (accurateMassMax - accurateMassMin > DELTA_MASS_LIMIT) {
			throw new InvalidParameterException("Difference between min and max is > " + DELTA_MASS_LIMIT);
		}
		return combinePages(compoundSearchServices.stream()
				.map(compoundSearchService -> compoundSearchService.findByAccurateMass(accurateMassMin, accurateMassMax, pageable)));
	}

	@Operation(summary = "Substances by ID", description = "At most " + PARAMETER_ID_LIMIT + " IDs per request; "
			+ "IDs without a substance are left out.")
	@GetMapping(params = {"id"})
	public Collection<WebserviceEntry> findByPublicID(
			@Parameter(description = "Substance IDs, repeat the parameter for several IDs", example = "SI00000528")
			@RequestParam List<String> id) {
		LOGGER.debug("entering method findByPublicID");
		if (id.size() > PARAMETER_ID_LIMIT) {
			throw new InvalidParameterException("To much requests");
		}

		Collection<WebserviceEntry> results = new ArrayList<>();
		for (String currentId : id) {
			compoundSearchServices.stream()
					.map(compoundSearchService -> compoundSearchService.findByPublicID(currentId))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.findFirst()
					.ifPresent(results::add);
		}
		return results;
	}

	@Operation(summary = "Substances by InChI key")
	@GetMapping(params = "inchiKey")
	public Page<WebserviceEntry> findByInchiKey(
			@Parameter(description = "Full InChI key", example = "QRUDEWIWKLJBPS-UHFFFAOYSA-N")
			@RequestParam String inchiKey) {
		List<WebserviceEntry> entries = compoundSearchServices.stream()
				.map(compoundSearchService -> compoundSearchService.findByInchiKeyValue(inchiKey))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toList());

		return new UnpagedPage<>(entries);
	}
}
