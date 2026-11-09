package de.hswt.fi.ui.flow.i18n;

import com.vaadin.flow.i18n.I18NProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Texts of the UI in German and English (replaces the vaadin4spring I18N). A missing key is shown as
 * {@code "!" + key}, as before.
 */
@Component
public class FiI18NProvider implements I18NProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(FiI18NProvider.class);

	public static final Locale GERMAN = Locale.GERMAN;

	public static final Locale ENGLISH = Locale.ENGLISH;

	private static final List<Locale> LOCALES = List.of(ENGLISH, GERMAN);

	private final ResourceBundleMessageSource messages = new ResourceBundleMessageSource();

	public FiI18NProvider() {
		messages.setBasenames("de.hswt.fi.ui.flow.i18n.messages");
		messages.setDefaultEncoding("UTF-8");
		// English is the fallback for all other languages, independent of the language of the server
		messages.setFallbackToSystemLocale(false);
		messages.setDefaultLocale(ENGLISH);
	}

	@Override
	public List<Locale> getProvidedLocales() {
		return LOCALES;
	}

	@Override
	public String getTranslation(String key, Locale locale, Object... params) {
		try {
			return messages.getMessage(key, params.length == 0 ? null : params, supported(locale));
		} catch (NoSuchMessageException e) {
			LOGGER.warn("Tried to retrieve message with code [{}] that does not exist", key);
			return "!" + key;
		}
	}

	/** German for all German locales, English otherwise. */
	public static Locale supported(Locale locale) {
		return locale != null && GERMAN.getLanguage().equals(locale.getLanguage()) ? GERMAN : ENGLISH;
	}
}
