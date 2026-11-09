package de.hswt.fi.ui.flow.views.processing;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.progressbar.ProgressBar;
import de.hswt.fi.processing.service.model.ProcessingUnit;
import de.hswt.fi.processing.service.model.ProcessingUnitState.UnitState;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Progress of a processing job (TaskWindow of the Vaadin 8 UI): one row per process with its state (waiting,
 * processing, finished). Cannot be closed by the user; it closes when the job ends.
 */
public class ProgressDialog extends Dialog {

	private final Map<ProcessingUnit, Span> states = new EnumMap<>(ProcessingUnit.class);

	private final Map<ProcessingUnit, Div> markers = new EnumMap<>(ProcessingUnit.class);

	public ProgressDialog(Set<ProcessingUnit> units) {
		setHeaderTitle(getTranslation("ui.processing.progress.title"));
		setWidth("min(28rem, 95vw)");
		setCloseOnEsc(false);
		setCloseOnOutsideClick(false);
		Div table = new Div();
		table.addClassName("fi-progress-table");
		for (ProcessingUnit unit : units) {
			Span state = new Span();
			Div marker = new Div();
			marker.addClassName("fi-progress-marker");
			states.put(unit, state);
			markers.put(unit, marker);
			table.add(new Span(ProcessingTexts.unit(this, unit)), marker, state);
			setState(unit, UnitState.IDLE);
		}
		add(table);
	}

	public void setState(ProcessingUnit unit, UnitState state) {
		Span label = states.get(unit);
		if (label == null) {
			return;
		}
		label.setText(getTranslation("ui.processing.progress." + state.name()));
		Div marker = markers.get(unit);
		marker.removeAll();
		if (state == UnitState.PROCESSING) {
			ProgressBar bar = new ProgressBar();
			bar.setIndeterminate(true);
			marker.add(bar);
		} else if (state == UnitState.FINISHED) {
			Icon done = VaadinIcon.CHECK.create();
			done.addClassName("fi-icon-ok");
			marker.add(done);
		}
	}
}
