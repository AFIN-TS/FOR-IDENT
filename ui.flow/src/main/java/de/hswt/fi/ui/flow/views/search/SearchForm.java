package de.hswt.fi.ui.flow.views.search;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import de.hswt.fi.common.Ionisation;
import de.hswt.fi.search.service.mass.search.model.SearchParameter;
import de.hswt.fi.ui.flow.search.SearchParameters;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * Search form of quick search (mass, ion species, logP, halogens) and search (additionally name, CAS, formula, InChI
 * key, IUPAC, SMILES and ID), as AbstractSearchFormComponent and SearchFormComponent of the Vaadin 8 UI. Mass and logP
 * accept a value with tolerance or a range.
 */
public class SearchForm extends VerticalLayout {

	static final List<Double> PPM_VALUES = List.of(1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0, 500.0, 1000.0, 1500.0,
			2000.0, 5000.0);

	/** 0.0 to 1.0 in steps of 0.1 (the Vaadin 8 UI listed them unordered, with rounding errors). */
	static final List<Double> DELTA_VALUES = IntStream.rangeClosed(0, 10).mapToObj(i -> i / 10.0).toList();

	static final List<String> HALOGENS = List.of("Cl", "Br", "F", "I");

	private final boolean detailed;

	private final TextField name = textField("ui.search.name");

	private final TextField cas = textField("ui.search.cas");

	private final TextField formula = textField("ui.search.formula");

	private final TextField inchiKey = textField("ui.search.inchiKey");

	private final TextField iupac = textField("ui.search.iupac");

	private final TextField smiles = textField("ui.search.smiles");

	private final TextField publicId = textField("ui.search.publicId");

	private final Select<Ionisation> ionisation = new Select<>();

	private final TextField mass = textField("ui.search.mass");

	private final ComboBox<Double> ppm = new ComboBox<>();

	private final TextField logP = textField("ui.search.logP");

	private final ComboBox<Double> delta = new ComboBox<>();

	private final CheckboxGroup<String> halogens = new CheckboxGroup<>();

	/**
	 * @param detailed with the name and identifier fields (search view)
	 * @param onSearch receives the parameters of a valid input
	 * @param onInvalid receives value and field caption of an invalid mass or logP input
	 */
	public SearchForm(boolean detailed, Consumer<SearchParameter> onSearch, BiConsumer<String, String> onInvalid) {
		this.detailed = detailed;
		setPadding(false);
		setSpacing(false);

		ionisation.setLabel(getTranslation("ui.search.ionisation"));
		ionisation.setItems(Ionisation.values());
		ionisation.setValue(Ionisation.NEUTRAL_IONISATION);
		ionisation.setWidthFull();
		mass.setHelperText(getTranslation("ui.search.massHint"));
		ppm.setLabel(getTranslation("ui.search.ppm"));
		ppm.setItems(PPM_VALUES);
		ppm.setItemLabelGenerator(SearchForm::formatNumber);
		ppm.setValue(PPM_VALUES.get(0));
		ppm.setAllowCustomValue(false);
		ppm.setWidth("7rem");
		logP.setHelperText(getTranslation("ui.search.logPHint"));
		delta.setLabel(getTranslation("ui.search.delta"));
		delta.setItems(DELTA_VALUES);
		delta.setItemLabelGenerator(SearchForm::formatNumber);
		delta.setValue(DELTA_VALUES.get(0));
		delta.setWidth("7rem");
		halogens.setLabel(getTranslation("ui.search.halogens"));
		halogens.setItems(HALOGENS);

		FormLayout form = new FormLayout();
		form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		if (detailed) {
			form.add(name, cas, formula, inchiKey, iupac, smiles, publicId);
		}
		// Tolerance next to the value; in a narrow column below it
		HorizontalLayout massRow = new HorizontalLayout(mass, ppm);
		massRow.setWidthFull();
		massRow.setWrap(true);
		massRow.expand(mass);
		mass.setMinWidth("9rem");
		HorizontalLayout logPRow = new HorizontalLayout(logP, delta);
		logPRow.setWidthFull();
		logPRow.setWrap(true);
		logPRow.expand(logP);
		logP.setMinWidth("9rem");
		form.add(ionisation, massRow, logPRow, halogens);

		Button search = new Button(getTranslation("ui.search.submit"), VaadinIcon.SEARCH.create(), e -> {
			SearchParameter parameter = read(onInvalid);
			if (parameter != null) {
				onSearch.accept(parameter);
			}
		});
		search.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		search.addClickShortcut(Key.ENTER).listenOn(this);
		Button clear = new Button(getTranslation("ui.search.clear"), VaadinIcon.ERASER.create(), e -> clear());
		add(form, new HorizontalLayout(search, clear));
	}

	private TextField textField(String key) {
		TextField field = new TextField(getTranslation(key));
		field.setClearButtonVisible(true);
		field.setWidthFull();
		return field;
	}

	/** Parameters of the form; null (after calling onInvalid) for an invalid mass or logP input. */
	private SearchParameter read(BiConsumer<String, String> onInvalid) {
		SearchParameter parameter = new SearchParameter();
		if (detailed) {
			parameter.setName(name.getValue());
			parameter.setCas(cas.getValue());
			parameter.setElementalFormula(formula.getValue());
			parameter.setInchiKey(inchiKey.getValue());
			parameter.setIupac(iupac.getValue());
			parameter.setSmiles(smiles.getValue());
			parameter.setPublicID(publicId.getValue());
		}
		parameter.setIonisation(ionisation.getValue());
		if (!SearchParameters.setMass(parameter, mass.getValue(), ppm.getValue() == null ? PPM_VALUES.get(0) : ppm.getValue())) {
			onInvalid.accept(mass.getValue(), mass.getLabel());
			return null;
		}
		if (!SearchParameters.setLogP(parameter, logP.getValue(), delta.getValue() == null ? 0.0 : delta.getValue())) {
			onInvalid.accept(logP.getValue(), logP.getLabel());
			return null;
		}
		parameter.setHalogens(new LinkedHashSet<>(halogens.getValue()));
		return parameter;
	}

	public void clear() {
		List.of(name, cas, formula, inchiKey, iupac, smiles, publicId, mass, logP).forEach(TextField::clear);
		ionisation.setValue(Ionisation.NEUTRAL_IONISATION);
		ppm.setValue(PPM_VALUES.get(0));
		delta.setValue(DELTA_VALUES.get(0));
		halogens.clear();
	}

	/** Shows the parameters of an earlier search. */
	public void show(SearchParameter parameter) {
		clear();
		name.setValue(nullToEmpty(parameter.getName()));
		cas.setValue(nullToEmpty(parameter.getCas()));
		formula.setValue(nullToEmpty(parameter.getElementalFormula()));
		inchiKey.setValue(nullToEmpty(parameter.getInchiKey()));
		iupac.setValue(nullToEmpty(parameter.getIupac()));
		smiles.setValue(nullToEmpty(parameter.getSmiles()));
		publicId.setValue(nullToEmpty(parameter.getPublicID()));
		if (parameter.getIonisation() != null) {
			ionisation.setValue(parameter.getIonisation());
		}
		if (parameter.getAccurateMass() != null) {
			mass.setValue(formatNumber(parameter.getAccurateMass()));
			ppm.setValue(PPM_VALUES.contains(parameter.getPpm()) ? parameter.getPpm() : PPM_VALUES.get(0));
		} else if (isRange(parameter.getAccurateMassRangeMin(), parameter.getAccurateMassRangeMax())) {
			mass.setValue(range(parameter.getAccurateMassRangeMin(), parameter.getAccurateMassRangeMax()));
		}
		if (parameter.getLogP() != null) {
			logP.setValue(formatNumber(parameter.getLogP()));
			delta.setValue(DELTA_VALUES.contains(parameter.getLogPDelta()) ? parameter.getLogPDelta() : DELTA_VALUES.get(0));
		} else if (isRange(parameter.getLogPRangeMin(), parameter.getLogPRangeMax())) {
			logP.setValue(range(parameter.getLogPRangeMin(), parameter.getLogPRangeMax()));
		}
		Set<String> selected = parameter.getHalogens();
		if (selected != null) {
			halogens.setValue(new LinkedHashSet<>(selected));
		}
	}

	/** Search by ID or InChI key, e.g. from a link (stoffidentid / inchikey). */
	public void setIdentifier(String publicIdValue, String inchiKeyValue) {
		clear();
		publicId.setValue(nullToEmpty(publicIdValue));
		inchiKey.setValue(nullToEmpty(inchiKeyValue));
	}

	private static boolean isRange(Double min, Double max) {
		return min != null && max != null
				&& (Double.compare(min, Double.MIN_VALUE) != 0 || Double.compare(max, Double.MAX_VALUE) != 0);
	}

	private static String range(Double min, Double max) {
		return max > 1e300 ? ">" + formatNumber(min) : formatNumber(min) + "-" + formatNumber(max);
	}

	static String formatNumber(Double value) {
		if (value == null) {
			return "";
		}
		String text = String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "");
		return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
