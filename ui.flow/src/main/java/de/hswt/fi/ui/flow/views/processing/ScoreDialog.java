package de.hswt.fi.ui.flow.views.processing;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.textfield.IntegerField;
import de.hswt.fi.processing.service.model.ProcessingUnit;
import de.hswt.fi.processing.service.model.ProcessingUnitState;
import de.hswt.fi.processing.service.model.ScoreSettings;
import de.hswt.fi.ui.flow.processing.ScoreWeights;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Selection of the processes and their score weights (ScoreWindow of the Vaadin 8 UI). The weights are whole percent
 * and free; if the weights of the active processes do not add up to 100 % on OK, a pop-up offers to adjust them
 * automatically (proportionally, see {@link ScoreWeights}) or to go back to editing (topic T1).
 */
public class ScoreDialog extends Dialog {

	private final ScoreSettings settings;

	private final Runnable onChange;

	private final Map<ProcessingUnit, ProcessingUnitState> states = new LinkedHashMap<>();

	private final Map<ProcessingUnit, Checkbox> activated = new LinkedHashMap<>();

	private final Map<ProcessingUnit, IntegerField> weights = new LinkedHashMap<>();

	private final Span sum = new Span();

	/**
	 * @param settings the score settings of the selected file, changed on OK
	 * @param showTp   whether transformation products can be processed (pathway database switched on)
	 */
	public ScoreDialog(ScoreSettings settings, boolean showTp, Runnable onChange) {
		this.settings = settings;
		this.onChange = onChange;
		setHeaderTitle(getTranslation("ui.processing.score.title"));
		setWidth("min(42rem, 95vw)");

		Div table = new Div();
		table.addClassName("fi-score-table");
		table.add(header("ui.processing.score.process"), header("ui.processing.score.dataAvailable"),
				header("ui.processing.score.activated"), header("ui.processing.score.weight"));
		Map<ProcessingUnit, Integer> percent = percent(settings);
		for (ProcessingUnitState state : states(settings, showTp)) {
			states.put(state.getProcessUnit(), state);
			addRow(table, state, percent.getOrDefault(state.getProcessUnit(), 0));
		}
		sum.addClassName("fi-score-sum");
		add(new Paragraph(getTranslation("ui.processing.score.description")), table, sum);
		updateSum();

		Button cancel = new Button(getTranslation("ui.button.cancel"), e -> close());
		Button ok = new Button(getTranslation("ui.button.ok"), e -> ok());
		ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		getFooter().add(cancel, ok);
	}

	/** The processes in the order of the dialog; transformation products only if they can be processed. */
	static List<ProcessingUnitState> states(ScoreSettings settings, boolean showTp) {
		return settings.getProcessingUnitStates().stream()
				.filter(state -> state != null && (showTp || state.getProcessUnit() != ProcessingUnit.TP))
				.sorted(Comparator.comparingInt(state -> order(state.getProcessUnit())))
				.toList();
	}

	private static int order(ProcessingUnit unit) {
		return switch (unit) {
			case MASS_SCREENING -> 0;
			case RTI_SCREENING -> 1;
			case MSMS -> 2;
			case TP -> 3;
			case MASSBANK_SIMPLE -> 4;
		};
	}

	/**
	 * Weights of the processes that count in whole percent. Weights that add up to 1 (e.g. 1/3 each, set by the
	 * service) become 100 % without rounding loss (34/33/33).
	 */
	static Map<ProcessingUnit, Integer> percent(ScoreSettings settings) {
		Map<ProcessingUnit, Double> counting = new LinkedHashMap<>();
		settings.getProcessingUnitStates().stream().filter(ScoreWeights::counts)
				.forEach(state -> counting.put(state.getProcessUnit(), state.getScoreWeight()));
		double total = counting.values().stream().mapToDouble(Double::doubleValue).sum();
		if (!counting.isEmpty() && Math.abs(total - 1) < 1e-6) {
			return ScoreWeights.autofill(counting);
		}
		Map<ProcessingUnit, Integer> result = new LinkedHashMap<>();
		counting.forEach((unit, weight) -> result.put(unit, (int) Math.round(weight * ScoreWeights.TOTAL)));
		return result;
	}

	private Span header(String key) {
		Span span = new Span(getTranslation(key));
		span.addClassName("fi-score-header");
		return span;
	}

	private void addRow(Div table, ProcessingUnitState state, int percent) {
		Span name = new Span(ProcessingTexts.unit(this, state.getProcessUnit()));
		Component available = ProcessingTexts.availableIcon(this, state.isDataAvailable());

		Checkbox active = new Checkbox(state.isEnabled());
		active.setAriaLabel(getTranslation("ui.processing.score.activated") + " " + name.getText());
		// Without data a process cannot run; the mass screening is the base of all others
		active.setEnabled(state.isDataAvailable() && state.getProcessUnit() != ProcessingUnit.MASS_SCREENING);
		activated.put(state.getProcessUnit(), active);

		IntegerField weight = new IntegerField();
		weight.setMin(0);
		weight.setMax(ScoreWeights.TOTAL);
		weight.setStepButtonsVisible(true);
		weight.setSuffixComponent(new Span("%"));
		weight.setWidth("9rem");
		weight.setAriaLabel(getTranslation("ui.processing.score.weight") + " " + name.getText());
		weight.setValue(state.isScoreable() ? percent : null);
		weight.addValueChangeListener(e -> updateSum());
		weights.put(state.getProcessUnit(), weight);

		active.addValueChangeListener(e -> {
			if (!e.getValue() && state.isScoreable()) {
				weight.setValue(0);
			}
			updateRow(state);
			updateSum();
		});
		updateRow(state);
		if (!state.isDataAvailable()) {
			name.addClassName("fi-disabled-text");
		}
		table.add(name, available, active, weight);
	}

	private void updateRow(ProcessingUnitState state) {
		// Transformation products have no score yet, they can only be switched on and off
		ProcessingUnit unit = state.getProcessUnit();
		weights.get(unit).setEnabled(state.isScoreable() && activated.get(unit).getValue());
	}

	/** Current weights of the processes that count (activated, with data and a score). */
	private Map<ProcessingUnit, Integer> countingWeights() {
		Map<ProcessingUnit, Integer> result = new LinkedHashMap<>();
		weights.forEach((unit, field) -> {
			ProcessingUnitState state = states.get(unit);
			if (state.isScoreable() && state.isDataAvailable() && activated.get(unit).getValue()) {
				result.put(unit, field.getValue() == null ? 0 : Math.max(0, field.getValue()));
			}
		});
		return result;
	}

	private void updateSum() {
		int total = ScoreWeights.sum(countingWeights());
		sum.setText(getTranslation("ui.processing.score.sum", total));
		sum.getElement().getClassList().set("fi-score-sum-wrong", total != ScoreWeights.TOTAL);
	}

	private void ok() {
		if (weights.values().stream().anyMatch(field -> field.isEnabled() && field.isInvalid())) {
			return;
		}
		Map<ProcessingUnit, Integer> counting = countingWeights();
		if (ScoreWeights.sum(counting) == ScoreWeights.TOTAL) {
			write(counting);
			return;
		}
		Map<ProcessingUnit, Integer> adjusted = ScoreWeights.autofill(counting);
		Dialog confirm = new Dialog();
		confirm.setHeaderTitle(getTranslation("ui.processing.score.autofill.title"));
		confirm.setWidth("min(30rem, 95vw)");
		UnorderedList preview = new UnorderedList();
		adjusted.forEach((unit, value) -> preview.add(new ListItem(
				ProcessingTexts.unit(this, unit) + ": " + counting.get(unit) + " % → " + value + " %")));
		confirm.add(new Paragraph(getTranslation("ui.processing.score.autofill.message", ScoreWeights.sum(counting))),
				preview);
		Button back = new Button(getTranslation("ui.processing.score.autofill.back"), e -> confirm.close());
		Button adjust = new Button(getTranslation("ui.processing.score.autofill.adjust"), e -> {
			adjusted.forEach((unit, value) -> weights.get(unit).setValue(value));
			confirm.close();
			write(adjusted);
		});
		adjust.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		confirm.getFooter().add(back, adjust);
		confirm.open();
	}

	private void write(Map<ProcessingUnit, Integer> counting) {
		states.forEach((unit, state) -> {
			if (state.isDataAvailable()) {
				state.setEnabled(activated.get(unit).getValue());
			}
			state.setScoreWeight(counting.getOrDefault(unit, 0) / (double) ScoreWeights.TOTAL);
		});
		onChange.run();
		close();
	}
}
