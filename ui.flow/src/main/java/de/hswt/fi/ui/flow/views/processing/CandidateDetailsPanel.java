package de.hswt.fi.ui.flow.views.processing;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import de.hswt.fi.calculation.service.api.CalculationService;
import de.hswt.fi.model.Peak;
import de.hswt.fi.model.Score;
import de.hswt.fi.msms.service.model.MsMsCandidate;
import de.hswt.fi.msms.service.model.MsMsCandidateFragment;
import de.hswt.fi.processing.service.model.ProcessCandidate;
import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.tp.model.Pathway;
import de.hswt.fi.ui.flow.components.MsChart;
import de.hswt.fi.ui.flow.search.SearchViewServices;
import de.hswt.fi.ui.flow.views.search.DetailsPanel;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Details of the selected candidate (ProcessingDetailsComponent and ProcessingResultDetailsComponent of the Vaadin 8
 * UI): the substance, the scores of the processes and, if available, the MS/MS spectrum with the fragments found by
 * MetFrag and the transformation products. In the Vaadin 8 UI the scores, the MS/MS spectrum and the transformation
 * products were shown below the row of the result list.
 */
public class CandidateDetailsPanel extends VerticalLayout {

	private static final int PRECURSOR_IMAGE_SIZE = 300;

	private static final int FRAGMENT_IMAGE_SIZE = 200;

	private final transient SearchViewServices services;

	private final TabSheet tabs = new TabSheet();

	private final DetailsPanel substance;

	public CandidateDetailsPanel(SearchViewServices services, Consumer<Entry> download, Consumer<Entry> report) {
		this.services = services;
		setSizeFull();
		setPadding(false);
		substance = new DetailsPanel(services, download, report);
		tabs.setSizeFull();
		add(tabs);
		setCandidate(null);
	}

	public void setCandidate(ProcessCandidate candidate) {
		int selected = Math.max(0, tabs.getSelectedIndex());
		while (tabs.getTabCount() > 0) {
			tabs.remove(tabs.getTabAt(0));
		}
		substance.setEntry(candidate == null ? null : candidate.getEntry());
		tabs.add(getTranslation("ui.processing.details.substance"), substance);
		if (candidate != null) {
			tabs.add(getTranslation("ui.processing.details.scoring"), scoring(candidate));
			MsMsCandidate msms = candidate.getMsMsCandidate();
			if (msms != null && msms.getFragments() != null) {
				tabs.add("MS/MS", new MsMsView(candidate));
			}
			if (candidate.getPathwayCandidate() != null) {
				tabs.add(getTranslation("ui.processing.details.tp"), transformationProducts(candidate));
			}
		}
		// The same kind of details stays selected when the user goes through the candidates
		tabs.setSelectedIndex(Math.min(selected, tabs.getTabCount() - 1));
	}

	private Component scoring(ProcessCandidate candidate) {
		Map<String, Score> scores = new LinkedHashMap<>();
		if (candidate.getMassSearchResult() != null) {
			scores.put("ui.processing.unit.MASS_SCREENING", candidate.getMassSearchResult().getScore());
		}
		if (candidate.getIndexSearchResult() != null) {
			scores.put("ui.processing.unit.RTI_SCREENING", candidate.getIndexSearchResult().getScore());
		}
		if (candidate.getMsMsCandidate() != null && candidate.getMsMsCandidate().getScore() != null) {
			scores.put("ui.processing.unit.MSMS", candidate.getMsMsCandidate().getScore());
		}
		if (candidate.getMassBankSimpleScore() != null) {
			scores.put("ui.processing.unit.MASSBANK_SIMPLE", candidate.getMassBankSimpleScore());
		}
		Div table = new Div();
		table.addClassName("fi-scoring-table");
		for (String key : List.of("ui.processing.details.process", "ui.processing.details.score",
				"ui.processing.details.weight", "ui.processing.details.weighted")) {
			Span header = new Span(getTranslation(key));
			header.addClassName("fi-scoring-header");
			table.add(header);
		}
		scores.forEach((key, score) -> table.add(new Span(getTranslation(key)),
				new Span(score == null ? "–" : format(score.getScoreValue())),
				new Span(score == null ? "–" : format(score.getWeight())),
				new Span(score == null ? "–" : format(score.getWeightedValue()))));
		Span combined = new Span(getTranslation("ui.processing.details.combined"));
		Span combinedValue = new Span(candidate.getScore() == null ? "–" : format(candidate.getScore().getScoreValue()));
		combined.addClassName("fi-scoring-total");
		combinedValue.addClassName("fi-scoring-total");
		table.add(combined, new Span(), new Span(), combinedValue);
		Paragraph description = new Paragraph(getTranslation("ui.processing.details.scoringDescription"));
		description.addClassName("fi-hint");
		VerticalLayout layout = new VerticalLayout(table, description);
		layout.setPadding(true);
		return layout;
	}

	private static String format(Double value) {
		return value == null || value.isNaN() || value < 0 ?"–" : String.format(Locale.ROOT, "%.2f", value);
	}

	private Component transformationProducts(ProcessCandidate candidate) {
		VerticalLayout layout = new VerticalLayout();
		for (Pathway pathway : candidate.getPathwayCandidate().getPathways().values()) {
			pathway.getCompounds().forEach(compound -> layout.add(new Span(compound.getName() + " ("
					+ compound.getFormula() + ", " + ProcessingTexts.number(compound.getNeutralMass(), 4) + ")")));
		}
		Paragraph license = new Paragraph(getTranslation("ui.processing.details.tpLicense"));
		license.addClassName("fi-hint");
		layout.add(license);
		return layout;
	}

	private Image structure(String smiles, int size, String alt) {
		if (smiles == null || !services.calculation().isAvailable()) {
			return null;
		}
		CalculationService calculation = services.calculation();
		byte[] image = calculation.getSmilesAsImage(smiles, size, size);
		if (image == null) {
			return null;
		}
		Image structure = new Image(DownloadHandler.fromInputStream(event -> new DownloadResponse(
				new ByteArrayInputStream(image), "structure.png", "image/png", image.length)), alt);
		structure.addClassName("fi-structure");
		return structure;
	}

	/** Tile with formula, mass and structure of a fragment or of the candidate. */
	private Div tile(String formula, double mass, String smiles, int size) {
		Div tile = new Div();
		tile.addClassName("fi-fragment");
		Span formulaLabel = new Span(formula);
		formulaLabel.addClassName("fi-fragment-formula");
		tile.add(formulaLabel, new Span(ProcessingTexts.number(mass, 4)));
		Image image = structure(smiles, size, formula);
		if (image != null) {
			tile.add(image);
		}
		return tile;
	}

	/**
	 * MS/MS spectrum of the candidate (MsMsDetailsComponent of the Vaadin 8 UI): peaks matched by an in silico fragment
	 * in green, the others in blue; the structure of the candidate, the fragment of the selected peak and all fragments.
	 * Clicking a peak or a fragment selects both.
	 */
	private class MsMsView extends VerticalLayout {

		private final MsChart chart = new MsChart("rel. Intensity");

		private final Div selection = new Div();

		private final Map<Double, MsMsCandidateFragment> fragmentsByMz = new HashMap<>();

		private final Map<Double, Peak> peaksByMz = new HashMap<>();

		MsMsView(ProcessCandidate candidate) {
			setPadding(true);
			MsMsCandidate msms = candidate.getMsMsCandidate();
			List<Peak> fragmentPeaks = msms.getFragments().stream().map(MsMsCandidateFragment::getPeak).toList();
			List<Peak> matching = new ArrayList<>();
			List<Peak> other = new ArrayList<>();
			for (Peak peak : msms.getTargetPeaks()) {
				boolean matched = fragmentPeaks.stream().anyMatch(fragment -> fragment.getMz() == peak.getMz());
				(matched ? matching : other).add(peak);
				peaksByMz.put(peak.getMz(), peak);
			}
			msms.getFragments().forEach(fragment -> fragmentsByMz.put(fragment.getPeak().getMz(), fragment));
			chart.setSeries(List.of(series("match with in silico", "#7AB800", matching),
					series("ms data", "#0058BC", other)));
			chart.addClickListener(point -> showFragment(point.x()));

			Div top = new Div();
			top.addClassName("fi-msms-top");
			Div candidateTile = new Div();
			candidateTile.addClassName("fi-msms-candidate");
			Entry entry = candidate.getEntry();
			Span name = new Span(entry.getName() == null ? "" : entry.getName().getValue());
			name.addClassName("fi-msms-caption");
			candidateTile.add(name);
			if (msms.getSmiles() != null && entry.getSmiles() != null) {
				candidateTile.add(tile(value(entry.getElementalFormula()),
						entry.getAccurateMass() == null ? 0 : entry.getAccurateMass().getValue(),
						entry.getSmiles().getValue(), PRECURSOR_IMAGE_SIZE));
			}
			Span selectionCaption = new Span(getTranslation("ui.processing.details.currentSelection"));
			selectionCaption.addClassName("fi-msms-caption");
			Div selectionTile = new Div(selectionCaption, selection);
			selectionTile.addClassName("fi-msms-candidate");
			top.add(chart, candidateTile, selectionTile);

			Span fragmentsCaption = new Span(getTranslation("ui.processing.details.fragments", msms.getFragments().size()));
			fragmentsCaption.addClassName("fi-msms-caption");
			Div fragments = new Div();
			fragments.addClassName("fi-fragments");
			List<MsMsCandidateFragment> list = new ArrayList<>(msms.getFragments());
			// Largest fragments first, as before
			java.util.Collections.reverse(list);
			for (MsMsCandidateFragment fragment : list) {
				Div fragmentTile = tile(fragment.getFormula(), fragment.getMass(), fragment.getSmiles(), FRAGMENT_IMAGE_SIZE);
				fragmentTile.addClickListener(e -> {
					showFragment(fragment.getPeak().getMz());
					Peak peak = fragment.getPeak();
					chart.find(peak.getMz(), peak.getRelativeIntensity()).ifPresent(chart::select);
				});
				fragments.add(fragmentTile);
			}
			add(top, fragmentsCaption, fragments);
		}

		private MsChart.Series series(String name, String color, List<Peak> peaks) {
			return new MsChart.Series(name, color, peaks.stream()
					.map(peak -> new MsChart.Point(peak.getMz(), peak.getRelativeIntensity())).toList());
		}

		private void showFragment(double mz) {
			selection.removeAll();
			MsMsCandidateFragment fragment = fragmentsByMz.get(mz);
			if (fragment != null) {
				selection.add(tile(fragment.getFormula(), fragment.getMass(), fragment.getSmiles(), PRECURSOR_IMAGE_SIZE));
			} else if (peaksByMz.containsKey(mz)) {
				selection.add(new Span(getTranslation("ui.processing.details.noFragment", ProcessingTexts.number(mz, 4))));
			}
		}

		private static String value(de.hswt.fi.search.service.mass.search.model.properties.StringValueProperty property) {
			return property == null ? "" : property.getValue();
		}
	}
}
