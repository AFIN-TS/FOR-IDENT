package de.hswt.fi.ui.flow.views;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.hswt.fi.search.service.search.api.CompoundSearchService;
import de.hswt.fi.ui.flow.search.SubstanceSearch;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Selection of the data sources (DatabaseSourceComponent of the Vaadin 8 UI): all databases, those the user may not
 * search are disabled; all available ones are selected at first.
 */
public class DatabaseSelection extends VerticalLayout {

	private final CheckboxGroup<CompoundSearchService> databases = new CheckboxGroup<>();

	public DatabaseSelection(SubstanceSearch search) {
		setPadding(false);
		setSpacing(false);
		List<CompoundSearchService> available = search.getAvailableDatabases();
		databases.setItems(search.getAllDatabases());
		databases.setItemLabelGenerator(CompoundSearchService::getDatasourceName);
		databases.setItemEnabledProvider(available::contains);
		databases.setValue(new LinkedHashSet<>(available));
		databases.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
		Checkbox all = new Checkbox(getTranslation("ui.search.selectAll"), true);
		all.addValueChangeListener(e -> {
			if (e.isFromClient()) {
				databases.setValue(e.getValue() ? new LinkedHashSet<>(available) : new LinkedHashSet<>());
			}
		});
		databases.addValueChangeListener(e -> all.setValue(e.getValue().containsAll(available)));
		add(all, databases);
	}

	/** The selected databases in their display order. */
	public List<CompoundSearchService> getSelected() {
		return databases.getListDataView().getItems().filter(databases.getValue()::contains).toList();
	}
}
