package de.hswt.fi.ui.flow.views.search;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.hswt.fi.search.service.mass.search.model.SearchParameter;
import de.hswt.fi.ui.flow.layout.MainLayout;
import de.hswt.fi.ui.flow.search.SearchHistory;
import de.hswt.fi.ui.flow.search.SearchViewServices;

import java.util.HashMap;
import java.util.Map;

/**
 * Alphabetical index: all substances whose name begins with a letter A-Z or a digit 1-9 (IndexSearchView of the
 * Vaadin 8 UI); open to everybody.
 */
@Route(value = "indexsearch", layout = MainLayout.class)
@AnonymousAllowed
public class IndexSearchView extends AbstractSearchView {

	private Map<Character, Button> buttons;

	public IndexSearchView(SearchViewServices services) {
		super(services);
	}

	@Override
	protected Component createForm() {
		buttons = new HashMap<>();
		Div letters = new Div();
		letters.addClassName("fi-index-buttons");
		for (char c = 'A'; c <= 'Z'; c++) {
			letters.add(indexButton(c));
		}
		Div digits = new Div();
		digits.addClassName("fi-index-buttons");
		for (char c = '1'; c <= '9'; c++) {
			digits.add(indexButton(c));
		}
		VerticalLayout form = new VerticalLayout(new Span(getTranslation("ui.search.index")), letters, digits);
		form.setPadding(false);
		return form;
	}

	private Button indexButton(char character) {
		Button button = new Button(String.valueOf(character), e -> search(new SearchParameter(Character.toLowerCase(character))));
		button.addThemeVariants(ButtonVariant.LUMO_SMALL);
		button.setMinWidth("0");
		buttons.put(Character.toLowerCase(character), button);
		return button;
	}

	@Override
	protected SearchHistory.Kind getKind() {
		return SearchHistory.Kind.INDEX;
	}

	@Override
	protected void showParameter(SearchParameter parameter) {
		buttons.values().forEach(button -> button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY));
		Button selected = buttons.get(parameter.getIndexChar());
		if (selected != null) {
			selected.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		}
	}

	@Override
	public String getViewTitle() {
		return getTranslation("ui.nav.indexSearch");
	}
}
