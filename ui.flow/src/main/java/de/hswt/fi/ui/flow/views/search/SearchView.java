package de.hswt.fi.ui.flow.views.search;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.hswt.fi.search.service.mass.search.model.SearchParameter;
import de.hswt.fi.ui.flow.layout.MainLayout;
import de.hswt.fi.ui.flow.search.SearchHistory;
import de.hswt.fi.ui.flow.search.SearchViewServices;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Search by name, CAS, formula, InChI key, IUPAC, SMILES, ID, mass, logP and halogens (SearchView of the Vaadin 8 UI);
 * open to everybody. Links with {@code ?stoffidentid=SI00000528} or {@code ?inchikey=...} search for that substance at
 * once (deep links of the Vaadin 8 UI, used by external pages).
 */
@Route(value = "search", layout = MainLayout.class)
@AnonymousAllowed
public class SearchView extends AbstractSearchView implements BeforeEnterObserver {

	public static final String PARAMETER_PUBLIC_ID = "stoffidentid";

	public static final String PARAMETER_INCHI_KEY = "inchikey";

	private SearchForm form;

	public SearchView(SearchViewServices services) {
		super(services);
	}

	@Override
	protected Component createForm() {
		form = new SearchForm(true, this::search,
				(value, field) -> warn("ui.search.invalidTitle", "ui.search.invalid", value, field));
		return form;
	}

	@Override
	protected SearchHistory.Kind getKind() {
		return SearchHistory.Kind.SEARCH;
	}

	@Override
	protected void showParameter(SearchParameter parameter) {
		form.show(parameter);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Map<String, List<String>> parameters = event.getLocation().getQueryParameters().getParameters();
		Optional<String> publicId = first(parameters, PARAMETER_PUBLIC_ID);
		Optional<String> inchiKey = first(parameters, PARAMETER_INCHI_KEY);
		if (publicId.isEmpty() && inchiKey.isEmpty()) {
			return;
		}
		SearchParameter parameter = new SearchParameter();
		publicId.ifPresentOrElse(parameter::setPublicID, () -> parameter.setInchiKey(inchiKey.get()));
		form.setIdentifier(parameter.getPublicID(), parameter.getInchiKey());
		search(parameter);
	}

	/** First non-empty value of a query parameter, ignoring the case of its name. */
	static Optional<String> first(Map<String, List<String>> parameters, String name) {
		return parameters.entrySet().stream()
				.filter(entry -> entry.getKey().equalsIgnoreCase(name))
				.flatMap(entry -> entry.getValue().stream())
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.findFirst();
	}

	/** True if the query parameters contain a deep link to a substance. */
	public static boolean isDeepLink(QueryParameters queryParameters) {
		Map<String, List<String>> parameters = queryParameters.getParameters();
		return first(parameters, PARAMETER_PUBLIC_ID).isPresent() || first(parameters, PARAMETER_INCHI_KEY).isPresent();
	}

	@Override
	public String getViewTitle() {
		return getTranslation("ui.nav.search");
	}
}
