package de.hswt.fi.ui.flow.views.processing;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.hswt.fi.model.Feature;
import de.hswt.fi.model.Peak;
import de.hswt.fi.ui.flow.components.MsChart;

import java.util.List;

/**
 * MS/MS spectrum of the selected feature (MsDetailsComponent of the Vaadin 8 UI): chart and list of the peaks.
 */
public class SpectrumPanel extends VerticalLayout {

	private final Span caption = new Span();

	private final MsChart chart = new MsChart("Intensity");

	private final Grid<Peak> peaks = new Grid<>();

	public SpectrumPanel() {
		setSizeFull();
		addClassName("fi-panel");
		H3 title = new H3(getTranslation("ui.processing.spectrum.title"));
		title.addClassName("fi-panel-header");
		peaks.addColumn(peak -> ProcessingTexts.number(peak.getMz(), 4)).setHeader("m/z")
				.setComparator(Peak::getMz);
		peaks.addColumn(peak -> ProcessingTexts.number(peak.getIntensity(), 2))
				.setHeader(getTranslation("ui.processing.spectrum.intensity")).setComparator(Peak::getIntensity);
		peaks.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
		add(title, caption, chart, peaks);
		expand(peaks);
		setFeature(null);
	}

	public void setFeature(Feature feature) {
		List<Peak> list = feature == null ? List.of() : feature.getPeaks();
		if (feature == null) {
			caption.setText(getTranslation("ui.processing.spectrum.select"));
		} else if (list.isEmpty()) {
			caption.setText(getTranslation("ui.processing.spectrum.noData", feature.getIdentifier()));
		} else {
			caption.setText(getTranslation("ui.processing.spectrum.feature", feature.getIdentifier()));
		}
		chart.setVisible(!list.isEmpty());
		peaks.setVisible(!list.isEmpty());
		chart.setSeries(List.of(new MsChart.Series("ms data", "#0058BC",
				list.stream().map(peak -> new MsChart.Point(peak.getMz(), peak.getIntensity())).toList())));
		peaks.setItems(list);
	}
}
