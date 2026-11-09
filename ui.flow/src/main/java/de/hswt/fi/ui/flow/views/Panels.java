package de.hswt.fi.ui.flow.views;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Building blocks shared by the views: a panel with a title, buttons in its header and a content area, and the small
 * icon buttons of the panel headers.
 */
public final class Panels {

	private Panels() {
	}

	/** Panel with a header (title and buttons) and the content below. */
	public static VerticalLayout panel(String title, Component content, Component... headerButtons) {
		H3 caption = new H3(title);
		HorizontalLayout header = new HorizontalLayout(caption);
		header.add(headerButtons);
		header.addClassName("fi-panel-header");
		header.setWidthFull();
		header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		header.expand(caption);
		VerticalLayout body = new VerticalLayout(content);
		body.setPadding(true);
		body.setSpacing(false);
		VerticalLayout panel = new VerticalLayout(header, body);
		panel.addClassName("fi-panel");
		panel.setPadding(false);
		panel.setSpacing(false);
		return panel;
	}

	/** Borderless icon button with a tooltip, as used in the panel headers. */
	public static Button iconButton(VaadinIcon icon, String tooltip, ComponentEventListener<ClickEvent<Button>> listener) {
		Button button = new Button(icon.create(), listener);
		button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
		button.setTooltipText(tooltip);
		button.setAriaLabel(tooltip);
		return button;
	}
}
