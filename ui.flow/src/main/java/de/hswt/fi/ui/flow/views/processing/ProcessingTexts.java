package de.hswt.fi.ui.flow.views.processing;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.hswt.fi.processing.service.model.ProcessingUnit;

import java.util.Locale;

/** Texts and icons shared by the parts of the molecular screening view. */
final class ProcessingTexts {

	private ProcessingTexts() {
	}

	/** Name of a process, e.g. "Mass Screening". */
	static String unit(Component component, ProcessingUnit unit) {
		return component.getTranslation("ui.processing.unit." + unit.name());
	}

	/** Green tick if the data for a process is available, red ban sign otherwise. */
	static Icon availableIcon(Component component, boolean available) {
		Icon icon = available ? VaadinIcon.CHECK.create() : VaadinIcon.BAN.create();
		icon.addClassName(available ? "fi-icon-ok" : "fi-icon-no");
		String text = component.getTranslation(available ? "ui.processing.score.available" : "ui.processing.score.notAvailable");
		icon.setTooltipText(text);
		icon.getElement().setAttribute("aria-label", text);
		return icon;
	}

	/** Number with the given decimals, empty for null. */
	static String number(Double value, int decimals) {
		return value == null || value.isNaN() ? "" : String.format(Locale.ROOT, "%." + decimals + "f", value);
	}
}
