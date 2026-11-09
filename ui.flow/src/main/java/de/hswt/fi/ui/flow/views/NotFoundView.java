package de.hswt.fi.ui.flow.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.hswt.fi.ui.flow.layout.MainLayout;

/**
 * Unknown page, or a page the user may not open (Vaadin answers both as "not found", so it does not reveal which
 * pages exist). Shown inside the normal frame with a link to the start page.
 */
@ParentLayout(MainLayout.class)
@AnonymousAllowed
public class NotFoundView extends VerticalLayout implements HasErrorParameter<NotFoundException>,
		MainLayout.HasViewTitle {

	@Override
	public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
		removeAll();
		add(new H2(getTranslation("ui.notFound")), new RouterLink(getTranslation("ui.nav.home"), HomeView.class));
		return HttpStatusCode.NOT_FOUND.getCode();
	}

	@Override
	public String getViewTitle() {
		return "";
	}
}
