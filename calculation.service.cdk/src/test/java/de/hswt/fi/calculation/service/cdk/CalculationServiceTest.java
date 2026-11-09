package de.hswt.fi.calculation.service.cdk;

import de.hswt.fi.calculation.service.api.CalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
public class CalculationServiceTest {

	private static final double EPSILON = 0.00001;

	private CalculationService calculationService;

	private boolean equals(Double expected, Double observed) {
		return Math.abs(expected - observed) < EPSILON;
	}

	@BeforeEach
	public void setUp() {
		calculationService = new CdkCalculationService();
	}

	@Test
	public void testDeriveMassFromFormula() {
		// Metropolsäure
		String formula = "C14H21NO4";
		assertTrue(equals(267.147058152, calculationService.getMassFromFormula(formula)));
		
		// Simeton
		formula = "C8H15N5O";
		assertTrue(equals(197.127660099, calculationService.getMassFromFormula(formula)));
		
		// Amantadin
		formula = "C10H17N";
		assertTrue(equals(151.136099544, calculationService.getMassFromFormula(formula)));
		
		// Chloridazon
		formula = "C10H8ClN3O";
		assertTrue(equals(221.035589556, calculationService.getMassFromFormula(formula)));
		
		// Diuron 
		formula = "C9H10Cl2N2O";
		assertTrue(equals(232.0170183, calculationService.getMassFromFormula(formula)));
		
		// Simazine
		formula = "C7H12ClN5";
		assertTrue(equals(201.078123064, calculationService.getMassFromFormula(formula)));
	}

	@Test
	public void structureImageTest() {

		// Diethofencarb
		String formula = "CCOc1ccc(NC(=O)OC(C)C)cc1OCC";

		byte[] strutureImage = calculationService.getSmilesAsImage(formula, 500, 500);
		assertTrue(strutureImage.length > 0);
	}

	@Test
	public void inchiFromSmilesTest() {
		// Diethofencarb
		assertEquals("InChI=1S/C14H21NO4/c1-5-17-12-8-7-11(9-13(12)18-6-2)15-14(16)19-10(3)4/h7-10H,5-6H2,1-4H3,(H,15,16)", calculationService.getInChi("CCOc1ccc(NC(=O)OC(C)C)cc1OCC"));
	}

	@Test
	public void inchiKeyFromSmilesTest() {
		// Diethofencarb
		assertEquals("LNJNFVJKDJYTEU-UHFFFAOYSA-N", calculationService.getInChiKey("CCOc1ccc(NC(=O)OC(C)C)cc1OCC"));
	}

	@Test
	public void molecularFormulaFromSmiles() {
		// Diethofencarb
		assertEquals("C14H21NO4", calculationService.getMolecularFormula("CCOc1ccc(NC(=O)OC(C)C)cc1OCC"));
	}
}
