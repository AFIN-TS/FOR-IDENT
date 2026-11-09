package de.hswt.fi.web.service.def.controller;

import org.springframework.data.domain.PageImpl;

import java.util.List;

/**
 * Page with all results, without paging. Keeps the JSON of the public API unchanged: since Spring Data 2.2
 * {@link PageImpl#getSize()} returns the number of results for unpaged pages instead of 0.
 */
class UnpagedPage<T> extends PageImpl<T> {

	private static final long serialVersionUID = 1L;

	UnpagedPage(List<T> content) {
		super(content);
	}

	@Override
	public int getSize() {
		return getPageable().isPaged() ? super.getSize() : 0;
	}
}
