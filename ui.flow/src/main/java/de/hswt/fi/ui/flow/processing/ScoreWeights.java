package de.hswt.fi.ui.flow.processing;

import de.hswt.fi.processing.service.model.ProcessingUnitState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Score weights of the processes in whole percent (topic T1). The weights of the active processes have to add up to
 * 100 %; the Vaadin 8 UI capped the sliders and silently disabled OK otherwise. Now the weights are free and a wrong sum
 * can be adjusted automatically: proportionally scaled to 100 %, rounded to whole percent by the largest remainder
 * method, or split equally if all weights are 0.
 */
public final class ScoreWeights {

	public static final int TOTAL = 100;

	private ScoreWeights() {
	}

	/** True if the weight of the process counts: activated, with data and scoreable (not the transformation products). */
	public static boolean counts(ProcessingUnitState state) {
		return state != null && state.isEnabled() && state.isDataAvailable() && state.isScoreable();
	}

	public static <K> int sum(Map<K, Integer> weights) {
		return weights.values().stream().mapToInt(Integer::intValue).sum();
	}

	/**
	 * Weights scaled proportionally to 100 % in whole percent; equal parts if all weights are 0. The order of the keys
	 * is kept; with equal remainders the earlier key gets the extra percent.
	 */
	public static <K> Map<K, Integer> autofill(Map<K, ? extends Number> weights) {
		Map<K, Integer> result = new LinkedHashMap<>();
		if (weights.isEmpty()) {
			return result;
		}
		double sum = weights.values().stream().mapToDouble(value -> Math.max(0, value.doubleValue())).sum();
		Map<K, Double> exact = new LinkedHashMap<>();
		weights.forEach((key, value) -> exact.put(key, sum == 0
				? (double) TOTAL / weights.size()
				: Math.max(0, value.doubleValue()) * TOTAL / sum));
		exact.forEach((key, value) -> result.put(key, (int) Math.floor(value + 1e-9)));

		int missing = TOTAL - sum(result);
		List<K> byRemainder = new ArrayList<>(exact.keySet());
		List<K> order = List.copyOf(exact.keySet());
		byRemainder.sort(Comparator.<K>comparingDouble(key -> exact.get(key) - result.get(key)).reversed()
				.thenComparingInt(order::indexOf));
		for (int i = 0; i < missing; i++) {
			K key = byRemainder.get(i % byRemainder.size());
			result.put(key, result.get(key) + 1);
		}
		return result;
	}
}
