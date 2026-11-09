package de.hswt.fi.ui.flow.views.processing;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.hswt.fi.model.Feature;
import de.hswt.fi.model.FeatureSet;
import de.hswt.fi.model.RTICalibrationData;

import java.util.List;
import java.util.function.Consumer;

/**
 * Content of the selected file (ProcessingDataComponentLC of the Vaadin 8 UI): the RTI calibration data, if any (invalid
 * rows in red), and the features. Selecting a feature shows its MS/MS spectrum.
 */
public class FeatureDataPanel extends VerticalLayout {

	private final H4 calibrationTitle = new H4();

	private final Grid<RTICalibrationData> calibration = new Grid<>();

	private final H4 featureTitle = new H4();

	private final Grid<Feature> features = new Grid<>();

	public FeatureDataPanel(Consumer<Feature> selection) {
		setSizeFull();
		setPadding(false);

		calibration.addColumn(RTICalibrationData::getIdentifier).setHeader(getTranslation("ui.processing.data.substance"))
				.setResizable(true).setFlexGrow(2);
		calibration.addColumn(data -> data.isValid() ? "☑" : "☐").setHeader(getTranslation("ui.processing.data.validRt"))
				.setFlexGrow(0).setWidth("7rem");
		calibration.addColumn(data -> ProcessingTexts.number(data.getRti(), 1)).setHeader("RTI").setFlexGrow(1);
		calibration.addColumn(data -> ProcessingTexts.number(data.getSignal(), 2)).setHeader("logD").setFlexGrow(1);
		calibration.addColumn(data -> ProcessingTexts.number(data.getMeanRt(), 2)).setHeader("RT").setFlexGrow(1);
		calibration.setPartNameGenerator(data -> data.isValid() ? null : "fi-invalid-row");
		calibration.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
		calibration.setHeight("14rem");

		features.addColumn(Feature::getIdentifier).setHeader(getTranslation("ui.processing.data.target"))
				.setResizable(true).setFlexGrow(2).setSortable(true).setComparator(Feature::getIdentifier);
		features.addColumn(feature -> ProcessingTexts.number(feature.getPrecursorMass(), 4))
				.setHeader(getTranslation("ui.processing.data.mass")).setFlexGrow(1)
				.setComparator(feature -> nullSafe(feature.getPrecursorMass()));
		features.addColumn(Feature::getNeutralFormula).setHeader(getTranslation("ui.processing.data.formula")).setFlexGrow(1);
		features.addColumn(feature -> feature.getFormulaDerivedMass() != null ? "☑" : "☐")
				.setHeader(getTranslation("ui.processing.data.massCalculated")).setFlexGrow(0).setWidth("9rem");
		features.addColumn(feature -> ProcessingTexts.number(feature.getRetentionTime(), 2)).setHeader("RT")
				.setFlexGrow(1).setComparator(feature -> nullSafe(feature.getRetentionTime()));
		features.addColumn(feature -> ProcessingTexts.number(feature.getRetentionTimeIndex(), 1)).setHeader("RTI")
				.setFlexGrow(1).setComparator(feature -> nullSafe(feature.getRetentionTimeIndex()));
		features.addColumn(feature -> ProcessingTexts.number(feature.getRetentionTimeSignal(), 2)).setHeader("logD")
				.setFlexGrow(1);
		features.addColumn(feature -> feature.getPeaks().size()).setHeader(getTranslation("ui.processing.data.peaks"))
				.setFlexGrow(0).setWidth("6rem").setComparator(feature -> feature.getPeaks().size());
		features.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
		features.setColumnReorderingAllowed(true);
		features.getColumns().forEach(column -> column.setResizable(true));
		features.setEmptyStateText(getTranslation("ui.processing.data.none"));
		features.asSingleSelect().addValueChangeListener(e -> selection.accept(e.getValue()));

		add(calibrationTitle, calibration, featureTitle, features);
		expand(features);
		setFeatureSet(null);
	}

	private static double nullSafe(Double value) {
		return value == null ? Double.NaN : value;
	}

	public void setFeatureSet(FeatureSet featureSet) {
		List<RTICalibrationData> calibrationData = featureSet == null || featureSet.getRtiCalibrationData() == null
				? List.of() : featureSet.getRtiCalibrationData();
		calibration.setItems(calibrationData);
		calibrationTitle.setText(getTranslation("ui.processing.data.calibration", calibrationData.size()));
		calibrationTitle.setVisible(!calibrationData.isEmpty());
		calibration.setVisible(!calibrationData.isEmpty());
		List<Feature> featureList = featureSet == null ? List.of() : featureSet.getFeatures();
		features.setItems(featureList);
		featureTitle.setText(featureSet == null ? getTranslation("ui.processing.data.noFile")
				: getTranslation("ui.processing.data.features", featureSet.getName(), featureList.size()));
	}
}
