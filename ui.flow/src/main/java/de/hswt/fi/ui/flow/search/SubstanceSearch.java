package de.hswt.fi.ui.flow.search;

import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.SearchParameter;
import de.hswt.fi.search.service.search.api.CompoundSearchService;
import de.hswt.fi.security.service.api.SecurityService;
import de.hswt.fi.security.service.model.Database;
import de.hswt.fi.security.service.model.RegisteredUser;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Substance search of the search views (SearchHandler of the Vaadin 8 UI): runs a search in the selected databases and
 * determines which databases the current user may search.
 */
@Component
public class SubstanceSearch {

	private final List<CompoundSearchService> searchServices;

	private final SecurityService securityService;

	public SubstanceSearch(List<CompoundSearchService> searchServices, SecurityService securityService) {
		this.searchServices = searchServices.stream()
				.sorted(Comparator.comparingInt(CompoundSearchService::getIndex))
				.toList();
		this.securityService = securityService;
	}

	/** All databases in their display order. */
	public List<CompoundSearchService> getAllDatabases() {
		return searchServices;
	}

	/**
	 * Databases the current user may search: the public ones, plus those assigned to an enabled, logged in user (as the
	 * Vaadin 8 UI did on login).
	 */
	public List<CompoundSearchService> getAvailableDatabases() {
		RegisteredUser user = securityService.getCurrentUser();
		Set<String> assigned = user != null && user.isEnabled()
				? user.getAccessibleDatabases().stream().map(Database::getName).collect(Collectors.toSet())
				: Set.of();
		return searchServices.stream()
				.filter(service -> service.isAccessible() || assigned.contains(service.getDatasourceName()))
				.toList();
	}

	/**
	 * Restricts the selected databases to the available ones and writes their names into the parameter (part of the
	 * search identity in the history).
	 */
	public List<CompoundSearchService> selectDatabases(SearchParameter parameter, List<CompoundSearchService> selected) {
		List<CompoundSearchService> available = getAvailableDatabases();
		List<CompoundSearchService> allowed = selected.stream().filter(available::contains).toList();
		parameter.setDatasourceNames(allowed.stream().map(CompoundSearchService::getDatasourceName).toList());
		return allowed;
	}

	/**
	 * Runs the search in the given databases (see {@link #selectDatabases}): by the first character of the name for an
	 * index search, otherwise by all parameters.
	 */
	public List<Entry> search(SearchParameter parameter, List<CompoundSearchService> allowed) {
		if (parameter.getIndexChar() != ' ') {
			return allowed.stream()
					.flatMap(service -> service.searchByNameFirstCharacter(parameter.getIndexChar()).stream())
					.toList();
		}
		return allowed.stream()
				.flatMap(service -> service.searchDynamic(parameter).stream())
				.toList();
	}
}
