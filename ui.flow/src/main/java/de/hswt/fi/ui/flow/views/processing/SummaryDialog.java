package de.hswt.fi.ui.flow.views.processing;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import de.hswt.fi.beans.BeanMappingConfiguration;
import de.hswt.fi.model.ResultSummary;
import de.hswt.fi.processing.service.model.ProcessResultSummary;

import java.util.Locale;

/**
 * Summary of a processing run (SummaryWindow of the Vaadin 8 UI): per process the figures it reports, e.g. the number
 * of located and unlocated features.
 */
public class SummaryDialog extends Dialog {

	public SummaryDialog(ProcessResultSummary summary, Locale locale) {
		setHeaderTitle(getTranslation("ui.processing.summary.title"));
		setWidth("min(30rem, 95vw)");
		if (summary != null) {
			for (ResultSummary resultSummary : summary.getResultSummaries()) {
				H4 title = new H4(BeanMappingConfiguration.translate(resultSummary.getTitleI18nKey(), locale));
				title.addClassName("fi-summary-title");
				Div table = new Div();
				table.addClassName("fi-summary-table");
				resultSummary.getI18nCaptionValueMap().forEach((key, value) -> {
					Span strong = new Span(String.valueOf(value));
					strong.addClassName("fi-summary-value");
					table.add(new Span(BeanMappingConfiguration.translate(key, locale)), strong);
				});
				add(title, table);
			}
		}
		Button ok = new Button(getTranslation("ui.button.ok"), e -> close());
		ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		getFooter().add(ok);
	}
}
