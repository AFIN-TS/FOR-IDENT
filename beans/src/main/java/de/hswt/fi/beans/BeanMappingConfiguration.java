package de.hswt.fi.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Provides the {@link BeanComponentMapper} (captions of the bean components, e.g. the column headings of the Excel
 * export). Until SP-10 it was defined by the Vaadin 8 UI and translated with the vaadin4spring I18N; the behaviour is
 * the same: all {@code de/hswt/fi/**}{@code /i18n/messages*} bundles of the modules (UTF-8), the default locale of the
 * JVM, and {@code "!" + key} for a missing key.
 */
@Configuration
public class BeanMappingConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(BeanMappingConfiguration.class);

	private static final String I18N_PACKAGE_ROOT = "/de/hswt/fi/";

	private static final String I18N_PATTERN = "classpath*:" + I18N_PACKAGE_ROOT + "**/i18n/messages*";

	// The bundles are found once (classpath scan)
	private static final Supplier<MessageSource> MESSAGES = new Supplier<>() {
		private MessageSource messages;

		@Override
		public synchronized MessageSource get() {
			if (messages == null) {
				messages = beanMessageSource();
			}
			return messages;
		}
	};

	@Bean
	public BeanComponentMapper beanComponentMapper(BeanComponentRegistry beanComponentRegistry) {
		return createMapper(beanComponentRegistry, Locale::getDefault);
	}

	/**
	 * Mapper with captions and number formats in the given language, e.g. the language of the user interface for an
	 * Excel export (the bean above uses the default locale of the JVM).
	 */
	public static BeanComponentMapper createMapper(BeanComponentRegistry registry, Supplier<Locale> locale) {
		MessageSource messages = MESSAGES.get();
		return new BeanComponentMapper(key -> getMessage(messages, key, locale.get()), locale, registry);
	}

	/** Text of a key of the module bundles (e.g. the captions of a processing summary) in the given language. */
	public static String translate(String key, Locale locale) {
		return getMessage(MESSAGES.get(), key, locale);
	}

	private static String getMessage(MessageSource messages, String key, Locale locale) {
		try {
			return messages.getMessage(key, null, locale);
		} catch (NoSuchMessageException e) {
			LOGGER.warn("Tried to retrieve message with code [{}] that does not exist", key);
			return "!" + key;
		}
	}

	private static MessageSource beanMessageSource() {
		Set<String> basenames = new TreeSet<>();
		try {
			for (Resource resource : new PathMatchingResourcePatternResolver().getResources(I18N_PATTERN)) {
				String path = resource.getURL().toString();
				basenames.add(path.substring(path.lastIndexOf(I18N_PACKAGE_ROOT) + 1, path.lastIndexOf("/messages"))
						.replace('/', '.') + ".messages");
			}
		} catch (IOException e) {
			LOGGER.error("Could not read the message bundles: {}", e.getMessage());
		}
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasenames(basenames.toArray(new String[0]));
		messageSource.setDefaultEncoding("UTF-8");
		return messageSource;
	}
}
