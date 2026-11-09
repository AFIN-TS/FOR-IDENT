package de.hswt.fi.ui.flow.search;

import de.hswt.fi.application.properties.ApplicationProperties;
import de.hswt.fi.beans.BeanComponentMapper;
import de.hswt.fi.beans.BeanComponentRegistry;
import de.hswt.fi.beans.BeanMappingConfiguration;
import de.hswt.fi.calculation.service.api.CalculationService;
import de.hswt.fi.export.service.excel.ExcelCreator;
import de.hswt.fi.mail.service.MailService;
import de.hswt.fi.security.service.api.SecurityService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Services used by the search views and their dialogs, in one place to keep the view constructors short.
 */
@Component
public class SearchViewServices {

	private final SubstanceSearch substanceSearch;

	private final ObjectProvider<SearchHistory> searchHistory;

	private final CalculationService calculationService;

	private final ApplicationProperties applicationProperties;

	private final BeanComponentRegistry beanComponentRegistry;

	private final ObjectProvider<MailService> mailService;

	private final SecurityService securityService;

	public SearchViewServices(SubstanceSearch substanceSearch, ObjectProvider<SearchHistory> searchHistory,
							  CalculationService calculationService, ApplicationProperties applicationProperties,
							  BeanComponentRegistry beanComponentRegistry, ObjectProvider<MailService> mailService,
							  SecurityService securityService) {
		this.substanceSearch = substanceSearch;
		this.searchHistory = searchHistory;
		this.calculationService = calculationService;
		this.applicationProperties = applicationProperties;
		this.beanComponentRegistry = beanComponentRegistry;
		this.mailService = mailService;
		this.securityService = securityService;
	}

	public SubstanceSearch search() {
		return substanceSearch;
	}

	/** Search history of the current session. */
	public SearchHistory history() {
		return searchHistory.getObject();
	}

	public CalculationService calculation() {
		return calculationService;
	}

	public ApplicationProperties properties() {
		return applicationProperties;
	}

	/** Mail service of the current session (session scoped). */
	public MailService mail() {
		return mailService.getObject();
	}

	public SecurityService security() {
		return securityService;
	}

	/** Bean mapper with column captions and number formats in the given language. */
	public BeanComponentMapper mapper(Locale locale) {
		return BeanMappingConfiguration.createMapper(beanComponentRegistry, () -> locale);
	}

	/** Excel export with column captions and number formats in the given language. */
	public ExcelCreator excelCreator(Locale locale) {
		return new ExcelCreator(calculationService, mapper(locale));
	}
}
