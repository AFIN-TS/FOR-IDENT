package de.hswt.fi.calculation.service.rti.def;

import de.hswt.fi.calculation.service.rti.api.RTICalculationService;
import de.hswt.fi.model.Feature;
import de.hswt.fi.model.RTICalibrationData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ActiveProfiles("test")
public class RTICalculationServiceTest {

	private static final double EPSILON = 0.00001;

	private RTICalculationService rtiCalculationService;

	private boolean equals(Double expected, Double observed) {
		return Math.abs(expected - observed) < EPSILON;
	}

	private List<RTICalibrationData> createCalibrationTestData() {

		List<RTICalibrationData> calibrationData = new ArrayList<>();

		calibrationData.add(new RTICalibrationData("Metformin", -0.92, 1.29));
		calibrationData.add(new RTICalibrationData("Chloridazon", 1.11, 6.46));
		calibrationData.add(new RTICalibrationData("Carbetamide", 1.65, 7.85));
		calibrationData.add(new RTICalibrationData("Monuron", 1.93, 8.3));
		calibrationData.add(new RTICalibrationData("Chlorbromuron", 2.85, 11.7));
		calibrationData.add(new RTICalibrationData("Metconazole", 3.59, 12.7));
		calibrationData.add(new RTICalibrationData("Diazinon", 4.19, 14.19));
		calibrationData.add(new RTICalibrationData("Quinoxyfen", 4.98, 15));
		calibrationData.add(new RTICalibrationData("Fenofibrate", 5.28, 15.94));
		calibrationData.add(new RTICalibrationData("Metobromuron", 2.24, 10.26));
		calibrationData.add(new RTICalibrationData("Dapson", 1.27, 6.33));
		calibrationData.add(new RTICalibrationData("Linuron", 2.3, 11.42));

		return calibrationData;
	}

	@BeforeEach
	public void setUp() {
		rtiCalculationService = new DefaultRTICalculationService();
	}

	@Test
	public void rtiIndexCalculationTest() {
		List<RTICalibrationData> calibrationData = createCalibrationTestData();
		rtiCalculationService.calculateRetentionTimeIndex(calibrationData);

		List<Double> referenceRTIs = Arrays.asList(50.00000 , 82.74194, 91.45161, 95.96774, 110.80645, 122.74194, 132.41935, 145.16129, 150.00000, 100.96774, 85.32258, 101.93548);

		for (int i = 0; i < calibrationData.size(); i++) {
			Assertions.assertTrue(equals(referenceRTIs.get(i), calibrationData.get(i).getRti()));
		}
	}

	@Test
	public void filterInvalidRtTest() {
		List<RTICalibrationData> calibrationData = createCalibrationTestData();
		rtiCalculationService.filterInvalidRTs(calibrationData);

		for (RTICalibrationData calibration : calibrationData) {

			if (calibration.getIdentifier().equals("Dapson")) {
				Assertions.assertFalse(calibration.isValid());
			} else {
				Assertions.assertTrue(calibration.isValid());
			}
		}
	}

	//TODO Replace with Henry Constant test
	@Test
	public void calculateLogDTest() {
		List<RTICalibrationData> calibrationData = createCalibrationTestData();

		List<Feature> targetFeatures = new ArrayList<>();
		targetFeatures.add(new Feature.Builder("Amantadin", 152.14326).withRetentionTime(5.08055).build());
		targetFeatures.add(new Feature.Builder("Atrazine", 197.1276).withRetentionTime(4.98357).build());
		targetFeatures.add(new Feature.Builder("Simazine", 202.0858).withRetentionTime(8.35664).build());
		targetFeatures.add(new Feature.Builder("Chloridazon", 222.0427).withRetentionTime(6.47153).build());

		rtiCalculationService.calculateRetentionTimeIndex(calibrationData);
		rtiCalculationService.calculateSignal(calibrationData, targetFeatures);

		List<Double> referenceRTIs = Arrays.asList(74.00579, 73.39161, 96.21493, 82.81418);
		List<Double> referenceLogDs = Arrays.asList(0.5683596, 0.5302798, 1.945326, 1.11448);

		for (int i = 0; i < targetFeatures.size(); i++) {
			Assertions.assertTrue(equals(referenceRTIs.get(i), targetFeatures.get(i).getRetentionTimeIndex()));
			Assertions.assertTrue(equals(referenceLogDs.get(i), targetFeatures.get(i).getRetentionTimeSignal()));
		}
	}
}
