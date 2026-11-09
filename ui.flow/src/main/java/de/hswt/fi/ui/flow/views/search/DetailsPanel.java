package de.hswt.fi.ui.flow.views.search;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import de.hswt.fi.application.properties.UIProperties;
import de.hswt.fi.common.ValueFormatUtil;
import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.Property;
import de.hswt.fi.search.service.mass.search.model.SourceList;
import de.hswt.fi.search.service.mass.search.model.properties.NumberValueProperty;
import de.hswt.fi.search.service.mass.search.model.properties.StringValueProperty;
import de.hswt.fi.ui.flow.search.SearchViewServices;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Details of the selected substance (AbstractDetailsComponent of the Vaadin 8 UI): structure, web search, general data,
 * Henry constants, categories, source lists, logP, logD, additional names, MassBank and external links. Source and date
 * of each value are shown with "Metadata" (on by default, as before).
 */
public class DetailsPanel extends VerticalLayout {

	private static final int STRUCTURE_IMAGE_SIZE = 400;

	private static final String WEB_SEARCH_PREFIX = "https://www.google.com/search?q=";

	private final transient SearchViewServices services;

	private final Div content = new Div();

	private final Checkbox metadata = new Checkbox();

	private Entry entry;

	public DetailsPanel(SearchViewServices services, Consumer<Entry> download, Consumer<Entry> report) {
		this.services = services;
		setSizeFull();
		setPadding(false);
		addClassName("fi-panel");

		Button downloadButton = iconButton(VaadinIcon.DOWNLOAD, "ui.search.download", () -> download.accept(entry));
		Button reportButton = iconButton(VaadinIcon.BUG, "ui.search.reportRecord", () -> report.accept(entry));
		metadata.setLabel(getTranslation("ui.details.metadata"));
		metadata.setValue(true);
		metadata.addValueChangeListener(e -> content.setClassName("fi-hide-metadata", !e.getValue()));
		H3 title = new H3(getTranslation("ui.details.title"));
		HorizontalLayout header = new HorizontalLayout(title, downloadButton, reportButton, metadata);
		header.addClassName("fi-panel-header");
		header.setWidthFull();
		header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		header.expand(title);

		content.addClassName("fi-details");
		content.setWidthFull();
		VerticalLayout scroller = new VerticalLayout(content);
		scroller.setPadding(true);
		scroller.getStyle().set("overflow", "auto");
		scroller.setSizeFull();
		add(header, scroller);
		expand(scroller);
		setEntry(null);
	}

	private Button iconButton(VaadinIcon icon, String tooltipKey, Runnable action) {
		Button button = new Button(icon.create(), e -> {
			if (entry != null) {
				action.run();
			}
		});
		button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
		button.setTooltipText(getTranslation(tooltipKey));
		button.setAriaLabel(getTranslation(tooltipKey));
		return button;
	}

	public void setEntry(Entry entry) {
		this.entry = entry;
		content.removeAll();
		if (entry == null) {
			content.add(new Paragraph(getTranslation("ui.details.none")));
			return;
		}
		addStructure();
		addSection("ui.details.webSearch", webSearch());
		addSection("ui.details.general", general());
		addSection("ui.details.henry", numbers(java.util.Arrays.asList(entry.getHenryBond(), entry.getHenryGroup(), entry.getHenryExper()),
				List.of("ui.entry.henryBond", "ui.entry.henryGroup", "ui.entry.henryExper")));
		addSection("ui.details.categories", entry.getCategories().stream()
				.sorted(Comparator.comparing(StringValueProperty::getValue, Comparator.nullsLast(String::compareToIgnoreCase)))
				.map(category -> property("ui.entry.category", category.getValue(), null, category.getLastModified(), null))
				.toList());
		addSection("ui.details.lists", entry.getSourceLists().stream()
				.sorted(Comparator.comparing(SourceList::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
				.map(this::sourceList).toList());
		addSection("ui.details.logP", entry.getLogpValues().stream().filter(value -> value.getValue() != null)
				.sorted(Comparator.comparing(NumberValueProperty::getValue))
				.map(value -> number("ui.entry.logP", value)).toList());
		addSection("ui.details.logD", entry.getLogdValues().stream().filter(value -> value.getValue() != null)
				.sorted(Comparator.comparing(NumberValueProperty::getPh, Comparator.nullsLast(Double::compare))
						.thenComparing(NumberValueProperty::getValue))
				.map(value -> number("ui.entry.logD", value)).toList());
		addSection("ui.details.additionalNames", entry.getAdditionalNames().stream()
				.sorted(Comparator.comparing(StringValueProperty::getValue, Comparator.nullsLast(String::compareToIgnoreCase)))
				.map(name -> string("ui.entry.additionalName", name)).toList());
		addSection("ui.details.massBank", entry.getMassBankIds().stream()
				.map(id -> (Component) link("MassBank EU ID: " + id.getValue(),
						links().getMassbankParameterized().replace("{value}", id.getValue())))
				.toList());
		addSection("ui.details.externalLinks", externalLinks());
	}

	private void addStructure() {
		StringValueProperty smiles = entry.getSmiles();
		if (smiles == null || smiles.getValue() == null || !services.calculation().isAvailable()) {
			return;
		}
		byte[] image = services.calculation().getSmilesAsImage(smiles.getValue(), STRUCTURE_IMAGE_SIZE,
				STRUCTURE_IMAGE_SIZE);
		if (image == null) {
			return;
		}
		Image structure = new Image(DownloadHandler.fromInputStream(event -> new DownloadResponse(
				new ByteArrayInputStream(image), "structure.png", "image/png", image.length)),
				getTranslation("ui.details.structure") + " " + value(entry.getName()));
		structure.addClassName("fi-structure");
		addSection("ui.details.structure", List.of(structure));
	}

	private List<Component> webSearch() {
		List<Component> links = new ArrayList<>();
		if (value(entry.getName()) != null) {
			links.add(link(getTranslation("ui.details.webSearch.name"), WEB_SEARCH_PREFIX + encode(value(entry.getName()))));
		}
		if (value(entry.getCas()) != null) {
			links.add(link(getTranslation("ui.details.webSearch.cas"), WEB_SEARCH_PREFIX + encode(value(entry.getCas()))));
		}
		if (value(entry.getInchi()) != null) {
			links.add(link(getTranslation("ui.details.webSearch.inchi"), WEB_SEARCH_PREFIX + encode(value(entry.getInchi()))));
		}
		return links;
	}

	private List<Component> general() {
		List<Component> properties = new ArrayList<>();
		addString(properties, "ui.entry.name", entry.getName());
		if (entry.getPublicID() != null) {
			properties.add(property("ui.entry.publicId", entry.getPublicID(), null, null, null));
		}
		addString(properties, "ui.entry.smiles", entry.getSmiles());
		addString(properties, "ui.entry.inchi", entry.getInchi());
		addString(properties, "ui.entry.inchiKey", entry.getInchiKey());
		addString(properties, "ui.entry.iupac", entry.getIupac());
		addString(properties, "ui.entry.formula", entry.getElementalFormula());
		if (entry.getAccurateMass() != null && entry.getAccurateMass().getValue() != null) {
			properties.add(number("ui.entry.mass", entry.getAccurateMass()));
		}
		addString(properties, "ui.entry.cas", entry.getCas());
		return properties;
	}

	private List<Component> externalLinks() {
		List<Component> result = new ArrayList<>();
		// ECHA CHEM searches by CAS number; without one the start page
		String cas = value(entry.getCas());
		result.add(link("ECHA CHEM (REACH)", cas == null ? links().getReach()
				: links().getReachParameterized().replace("{value}", encode(cas))));
		if (entry.getDtxsid() != null) {
			result.add(link("EPA CompTox Dashboard", links().getEpa() + encode(entry.getDtxsid())));
		}
		String inchiKey = value(entry.getInchiKey());
		if (inchiKey != null) {
			result.add(link("PubChem", links().getPubchemParameterized().replace("{value}", encode(inchiKey))));
		}
		return result;
	}

	private UIProperties.Links links() {
		return services.properties().getUi().getLinks();
	}

	private void addString(List<Component> properties, String captionKey, StringValueProperty property) {
		if (value(property) != null) {
			properties.add(string(captionKey, property));
		}
	}

	private List<Component> numbers(List<NumberValueProperty> values, List<String> captionKeys) {
		List<Component> properties = new ArrayList<>();
		for (int i = 0; i < values.size(); i++) {
			if (values.get(i) != null && values.get(i).getValue() != null) {
				properties.add(number(captionKeys.get(i), values.get(i)));
			}
		}
		return properties;
	}

	private Component string(String captionKey, StringValueProperty property) {
		return property(captionKey, property.getValue(), property.getSource(), property.getLastModified(), null);
	}

	private Component number(String captionKey, NumberValueProperty property) {
		String ph = property.getPh() == null ? null : ValueFormatUtil.formatForPh(property.getPh(), getLocale());
		return property(captionKey, formatNumber(property.getValue()), ((Property<?>) property).getSource(),
				((Property<?>) property).getLastModified(), ph);
	}

	private String formatNumber(Double value) {
		return value == null ? "" : String.format(getLocale(), "%.4f", value);
	}

	private Component sourceList(SourceList sourceList) {
		Div item = property("ui.details.lists", sourceList.getName(), null, null, null);
		if (sourceList.getDescription() != null && !sourceList.getDescription().isBlank()) {
			item.add(meta("ui.details.description", sourceList.getDescription()));
		}
		return item;
	}

	private Div property(String captionKey, String value, String source, java.util.Date lastModified, String ph) {
		Div item = new Div();
		item.addClassName("fi-property");
		Span caption = new Span(getTranslation(captionKey));
		caption.addClassName("fi-property-caption");
		Span text = new Span(value == null ? "" : value);
		text.addClassName("fi-property-value");
		item.add(caption, text);
		if (ph != null) {
			item.add(meta("ui.details.ph", ph));
		}
		if (source != null && !source.isBlank()) {
			item.add(meta("ui.details.source", source));
		}
		if (lastModified != null) {
			item.add(meta("ui.details.lastModified", new java.text.SimpleDateFormat("dd.MM.yyyy").format(lastModified)));
		}
		return item;
	}

	private Div meta(String captionKey, String value) {
		Div line = new Div(new Span(getTranslation(captionKey) + ": "), new Span(value));
		line.addClassName("fi-property-meta");
		return line;
	}

	private void addSection(String titleKey, List<? extends Component> items) {
		if (items.isEmpty()) {
			return;
		}
		Div body = new Div();
		items.forEach(body::add);
		Details section = new Details(getTranslation(titleKey), body);
		section.setOpened(true);
		section.setWidthFull();
		section.addClassName("fi-details-section");
		content.add(section);
	}

	private static Anchor link(String text, String href) {
		Anchor anchor = new Anchor(href, text, AnchorTarget.BLANK);
		anchor.getElement().setAttribute("rel", "noopener noreferrer");
		anchor.getStyle().set("display", "block");
		return anchor;
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	static String value(StringValueProperty property) {
		return property == null || property.getValue() == null || property.getValue().isBlank()
				? null : property.getValue();
	}

	@Override
	public Locale getLocale() {
		return super.getLocale() == null ? Locale.ENGLISH : super.getLocale();
	}
}
