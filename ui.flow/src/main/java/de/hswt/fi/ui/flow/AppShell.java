package de.hswt.fi.ui.flow;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.lumo.Lumo;
import de.hswt.fi.application.properties.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Settings of the HTML page of the Vaadin Flow UI: Lumo theme with the FOR-IDENT colours, server push (progress of the
 * processing, SP-13), title and icon.
 */
@Push
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("styles/fi.css")
public class AppShell implements AppShellConfigurator {

	@Autowired
	private transient ApplicationProperties applicationProperties;

	@Override
	public void configurePage(AppShellSettings settings) {
		settings.setPageTitle(applicationProperties.getUi().getHeader().getCaption());
		settings.addFavIcon("icon", "img/favicon.ico", "16x16");
	}
}
