package de.hswt.fi.ui.flow.search;

import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import de.hswt.fi.search.service.mass.search.model.Entry;
import de.hswt.fi.search.service.mass.search.model.SearchParameter;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Searches of the session (search history of the Vaadin 8 UI), shared by quick search, search and index search. A
 * search that is repeated with the same parameters is taken from the history instead of the database.
 */
@Component
@VaadinSessionScope
public class SearchHistory {

	/** Number of searches kept; the oldest is removed first. */
	static final int MAX_SEARCHES = 30;

	public enum Kind {
		QUICK, SEARCH, INDEX
	}

	public record Run(Kind kind, SearchParameter parameter, List<Entry> results, LocalTime time) {
	}

	private final List<Run> runs = new ArrayList<>();

	public synchronized List<Run> getRuns() {
		return List.copyOf(runs);
	}

	/** Earlier search with the same parameters (including the selected databases). */
	public synchronized Optional<Run> find(SearchParameter parameter) {
		return runs.stream().filter(run -> run.parameter().equals(parameter)).findFirst();
	}

	/** Adds a search with results at the top; searches without results are not kept (as before). */
	public synchronized void add(Run run) {
		if (run.results().isEmpty()) {
			return;
		}
		runs.removeIf(existing -> existing.parameter().equals(run.parameter()));
		runs.add(0, run);
		while (runs.size() > MAX_SEARCHES) {
			runs.remove(runs.size() - 1);
		}
	}

	public synchronized void clear() {
		runs.clear();
	}
}
