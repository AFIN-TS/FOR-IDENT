package de.hswt.fi.ui.flow.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import de.hswt.fi.security.service.api.SecurityService;
import de.hswt.fi.security.service.model.Group;
import de.hswt.fi.security.service.model.RegisteredUser;
import de.hswt.fi.ui.flow.dialogs.UserEditDialog;
import de.hswt.fi.ui.flow.layout.MainLayout;
import de.hswt.fi.ui.flow.mail.AccountMails;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * User administration for administrators (UserAdministrationView of the Vaadin 8 UI): list of all accounts, add,
 * edit (enable, groups, databases, password) and delete.
 */
@Route(value = "admin/users", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class UserAdministrationView extends VerticalLayout implements HasDynamicTitle, MainLayout.HasViewTitle {

	private final transient SecurityService securityService;

	private final transient ObjectProvider<UserEditDialog> userEditDialog;

	private final transient AccountMails accountMails;

	private final String mailHost;

	private final Grid<RegisteredUser> grid = new Grid<>();

	private final TextField filter = new TextField();

	private ListDataProvider<RegisteredUser> users;

	public UserAdministrationView(SecurityService securityService, ObjectProvider<UserEditDialog> userEditDialog,
								  AccountMails accountMails, @Value("${spring.mail.host:}") String mailHost) {
		this.securityService = securityService;
		this.userEditDialog = userEditDialog;
		this.accountMails = accountMails;
		this.mailHost = mailHost;
		setSizeFull();

		filter.setPlaceholder(getTranslation("ui.admin.filter"));
		filter.setPrefixComponent(VaadinIcon.SEARCH.create());
		filter.setClearButtonVisible(true);
		filter.setValueChangeMode(ValueChangeMode.LAZY);
		filter.addValueChangeListener(e -> applyFilter());

		Button add = new Button(getTranslation("ui.admin.add"), VaadinIcon.PLUS.create(), e -> edit(null));
		add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		Button edit = new Button(getTranslation("ui.admin.edit"), VaadinIcon.EDIT.create(),
				e -> grid.getSelectedItems().stream().findFirst().ifPresent(this::edit));
		Button delete = new Button(getTranslation("ui.admin.delete"), VaadinIcon.TRASH.create(),
				e -> grid.getSelectedItems().stream().findFirst().ifPresent(this::confirmDelete));
		delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
		edit.setEnabled(false);
		delete.setEnabled(false);

		Button testMail = new Button(getTranslation("ui.admin.testMail"), VaadinIcon.ENVELOPE_O.create(), e -> sendTestMail());
		testMail.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		testMail.setTooltipText(getTranslation("ui.admin.testMail.tooltip"));

		HorizontalLayout toolbar = new HorizontalLayout(filter, add, edit, delete, testMail);
		toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
		toolbar.setWrap(true);

		grid.addColumn(RegisteredUser::getUsername).setHeader(getTranslation("ui.user.username"))
				.setSortable(true).setFlexGrow(2).setResizable(true);
		grid.addColumn(RegisteredUser::getMail).setHeader(getTranslation("ui.user.email"))
				.setSortable(true).setFlexGrow(2).setResizable(true);
		grid.addColumn(RegisteredUser::getFirstname).setHeader(getTranslation("ui.user.firstname")).setSortable(true);
		grid.addColumn(RegisteredUser::getLastname).setHeader(getTranslation("ui.user.lastname")).setSortable(true);
		grid.addColumn(RegisteredUser::getOrganisation).setHeader(getTranslation("ui.user.organisation"))
				.setSortable(true).setFlexGrow(2);
		grid.addColumn(user -> getTranslation(user.isEnabled() ? "ui.user.enabled" : "ui.user.disabled"))
				.setHeader(getTranslation("ui.user.enabled")).setSortable(true);
		grid.addColumn(user -> user.getGroups().stream().map(Group::getName).sorted().collect(Collectors.joining(", ")))
				.setHeader(getTranslation("ui.user.groups")).setFlexGrow(2);
		grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
		grid.setSizeFull();
		grid.addSelectionListener(e -> {
			boolean selected = e.getFirstSelectedItem().isPresent();
			edit.setEnabled(selected);
			delete.setEnabled(selected);
		});
		grid.addItemDoubleClickListener(e -> edit(e.getItem()));

		add(toolbar, grid);
		expand(grid);
		refresh();
	}

	/** Sends a test mail to the logged in administrator, to check the mail settings (FI_MAIL_*). */
	private void sendTestMail() {
		RegisteredUser user = securityService.getCurrentUser();
		if (mailHost == null || mailHost.isBlank()) {
			show(getTranslation("ui.admin.testMail.notConfigured"), NotificationVariant.LUMO_WARNING);
		} else if (user == null || user.getMail() == null || user.getMail().isBlank()) {
			show(getTranslation("ui.admin.testMail.noAddress"), NotificationVariant.LUMO_WARNING);
		} else {
			try {
				accountMails.sendTestMail(user.getMail(), getLocale());
				show(getTranslation("ui.admin.testMail.sent", user.getMail()), NotificationVariant.LUMO_SUCCESS);
			} catch (RuntimeException ex) {
				show(getTranslation("ui.admin.testMail.failed", rootMessage(ex)), NotificationVariant.LUMO_ERROR);
			}
		}
	}

	private static String rootMessage(Throwable throwable) {
		Throwable cause = throwable;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
	}

	private static void show(String text, NotificationVariant variant) {
		Notification.show(text, 8000, Notification.Position.MIDDLE).addThemeVariants(variant);
	}

	private void refresh() {
		users = new ListDataProvider<>(new ArrayList<>(securityService.findAllUsers()));
		grid.setDataProvider(users);
		applyFilter();
	}

	private void applyFilter() {
		String text = filter.getValue().trim().toLowerCase(Locale.ROOT);
		users.setFilter(user -> text.isEmpty() || Stream.of(user.getUsername(), user.getMail(), user.getFirstname(),
						user.getLastname(), user.getOrganisation())
				.filter(Objects::nonNull)
				.anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(text)));
	}

	private void edit(RegisteredUser user) {
		userEditDialog.getObject().open(user, this::refresh);
	}

	private void confirmDelete(RegisteredUser user) {
		ConfirmDialog dialog = new ConfirmDialog();
		dialog.setHeader(getTranslation("ui.admin.deleteTitle"));
		dialog.setText(getTranslation("ui.admin.deleteConfirm", user.getUsername()));
		dialog.setCancelable(true);
		dialog.setCancelText(getTranslation("ui.button.cancel"));
		dialog.setConfirmText(getTranslation("ui.button.delete"));
		dialog.setConfirmButtonTheme("error primary");
		dialog.addConfirmListener(e -> {
			if (securityService.deleteUser(user)) {
				com.vaadin.flow.component.notification.Notification.show(getTranslation("ui.admin.deleted"));
			} else {
				com.vaadin.flow.component.notification.Notification.show(getTranslation("ui.admin.error"));
			}
			refresh();
		});
		dialog.open();
	}

	@Override
	public String getViewTitle() {
		return getTranslation("ui.admin.title");
	}

	@Override
	public String getPageTitle() {
		return getTranslation("ui.admin.title");
	}
}
