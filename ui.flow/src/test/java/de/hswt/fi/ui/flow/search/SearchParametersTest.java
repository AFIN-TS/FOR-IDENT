package de.hswt.fi.ui.flow.search;

import de.hswt.fi.common.Ionisation;
import de.hswt.fi.search.service.mass.search.model.SearchParameter;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchParametersTest {

	@Test
	public void massValueUsesPpmAndCommaDecimal() {
		SearchParameter parameter = new SearchParameter();
		assertTrue(SearchParameters.setMass(parameter, " 189,1022 ", 5.0));
		assertEquals(189.1022, parameter.getAccurateMass());
		assertEquals(5.0, parameter.getPpm());
		assertFalse(SearchParameters.isEmpty(parameter));
	}

	@Test
	public void massRangeAndGreaterThan() {
		SearchParameter parameter = new SearchParameter();
		assertTrue(SearchParameters.setMass(parameter, "188-189", 5.0));
		assertNull(parameter.getAccurateMass());
		assertEquals(188.0, parameter.getAccurateMassRangeMin());
		assertEquals(189.0, parameter.getAccurateMassRangeMax());
		// A range alone is a valid search (the Vaadin 8 UI rejected it as "missing parameter")
		assertFalse(SearchParameters.isEmpty(parameter));
		assertTrue(SearchParameters.setMass(parameter, ">500", 5.0));
		assertTrue(SearchParameters.describe(parameter, key -> key).contains("> 500"));
	}

	@Test
	public void invalidInputIsRejected() {
		SearchParameter parameter = new SearchParameter();
		assertFalse(SearchParameters.setMass(parameter, "abc", 5.0));
		assertFalse(SearchParameters.setLogP(parameter, "1..2", 0.1));
	}

	@Test
	public void emptyMassClearsAnEarlierValue() {
		SearchParameter parameter = new SearchParameter();
		SearchParameters.setMass(parameter, "200", 5.0);
		assertTrue(SearchParameters.setMass(parameter, "", 5.0));
		assertNull(parameter.getAccurateMass());
		assertTrue(SearchParameters.isEmpty(parameter));
	}

	@Test
	public void emptyParameterOnlyWithoutAnyRestriction() {
		SearchParameter parameter = new SearchParameter();
		assertTrue(SearchParameters.isEmpty(parameter));
		parameter.setHalogens(Set.of("Cl"));
		assertFalse(SearchParameters.isEmpty(parameter));
		assertFalse(SearchParameters.isEmpty(new SearchParameter('a')));
	}

	@Test
	public void descriptionListsTheSetFields() {
		SearchParameter parameter = new SearchParameter();
		parameter.setName("phenazone");
		parameter.setIonisation(Ionisation.POSITIVE_IONISATION);
		SearchParameters.setMass(parameter, "189.1022", 5.0);
		assertEquals("ui.search.name: phenazone; ui.search.mass: 189.1022 ± 5 ppm (+H)",
				SearchParameters.describe(parameter, key -> key));
	}
}
