package de.hswt.fi.ui.flow.processing;

import de.hswt.fi.processing.service.model.ProcessingUnit;
import de.hswt.fi.processing.service.model.ProcessingUnitState;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScoreWeightsTest {

	@Test
	public void scalesProportionally() {
		// 60 + 60 = 120 % -> 50 / 50
		assertEquals(List.of(50, 50), values(ScoreWeights.autofill(weights(60, 60))));
		// 20 + 30 = 50 % -> 40 / 60
		assertEquals(List.of(40, 60), values(ScoreWeights.autofill(weights(20, 30))));
	}

	@Test
	public void roundsByLargestRemainder() {
		// 10/10/10 -> 33.33 each: the first gets the missing percent
		assertEquals(List.of(34, 33, 33), values(ScoreWeights.autofill(weights(10, 10, 10))));
		// 50/30/40 = 120 -> 41.67 / 25 / 33.33 -> 42 / 25 / 33
		assertEquals(List.of(42, 25, 33), values(ScoreWeights.autofill(weights(50, 30, 40))));
		// default weights of the service (1/3 each) -> 100 in whole percent
		assertEquals(List.of(34, 33, 33), values(ScoreWeights.autofill(weights(1 / 3d, 1 / 3d, 1 / 3d))));
	}

	@Test
	public void splitsEquallyIfAllAreZero() {
		assertEquals(List.of(34, 33, 33), values(ScoreWeights.autofill(weights(0, 0, 0))));
		assertEquals(List.of(25, 25, 25, 25), values(ScoreWeights.autofill(weights(0, 0, 0, 0))));
	}

	@Test
	public void keepsACorrectSumAndZeroWeights() {
		assertEquals(List.of(70, 0, 30), values(ScoreWeights.autofill(weights(70, 0, 30))));
		assertEquals(List.of(100), values(ScoreWeights.autofill(weights(5))));
		assertTrue(ScoreWeights.autofill(new LinkedHashMap<String, Integer>()).isEmpty());
	}

	@Test
	public void alwaysAddsUpTo100() {
		for (int a = 0; a <= 100; a += 7) {
			for (int b = 0; b <= 100; b += 11) {
				for (int c = 0; c <= 100; c += 13) {
					assertEquals(100, ScoreWeights.sum(ScoreWeights.autofill(weights(a, b, c))), a + "/" + b + "/" + c);
				}
			}
		}
	}

	@Test
	public void onlyActiveScoreableProcessesWithDataCount() {
		ProcessingUnitState mass = new ProcessingUnitState(ProcessingUnit.MASS_SCREENING, 0.5, true);
		mass.setDataAvailable(true);
		assertTrue(ScoreWeights.counts(mass));
		mass.setEnabled(false);
		assertFalse(ScoreWeights.counts(mass));
		ProcessingUnitState noData = new ProcessingUnitState(ProcessingUnit.MSMS, 0.5, true);
		noData.setEnabled(true);
		assertFalse(ScoreWeights.counts(noData));
		ProcessingUnitState tp = new ProcessingUnitState(ProcessingUnit.TP, 0, false);
		tp.setDataAvailable(true);
		assertFalse(ScoreWeights.counts(tp));
	}

	private static Map<String, Double> weights(double... values) {
		Map<String, Double> weights = new LinkedHashMap<>();
		for (int i = 0; i < values.length; i++) {
			weights.put("p" + i, values[i]);
		}
		return weights;
	}

	private static List<Integer> values(Map<String, Integer> weights) {
		return List.copyOf(weights.values());
	}
}
