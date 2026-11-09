package de.hswt.fi.ui.flow.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The MS spectrum chart of the Vaadin 8 UI (MsChart) as a Vaadin Flow component (SP-09 spike, SP-13). The drawing code
 * (frontend/ms-chart/js-ms-chart.js) is taken over unchanged; only the server connection is new. A click on a bar
 * selects it and is reported to the click listeners.
 */
@Tag("div")
@JsModule("./ms-chart/js-ms-chart.js")
public class MsChart extends Component implements HasSize {

	public record Point(String id, double x, double y) implements Serializable {
		public Point(double x, double y) {
			this(UUID.randomUUID().toString(), x, y);
		}
	}

	public record Series(String id, String name, String color, List<Point> points) implements Serializable {
		public Series(String name, String color, List<Point> points) {
			this(UUID.randomUUID().toString(), name, color, List.copyOf(points));
		}
	}

	private final List<Series> series = new ArrayList<>();

	private final List<Consumer<Point>> clickListeners = new ArrayList<>();

	private final String axisCaptionY;

	private Point selected;

	/** Chart with the colours of the MS details of the Vaadin 8 UI; the y axis shows the given caption. */
	public MsChart(String axisCaptionY) {
		this.axisCaptionY = axisCaptionY;
		addClassName("fi-ms-chart");
		setWidthFull();
		setHeight("300px");
	}

	public void setSeries(List<Series> newSeries) {
		series.clear();
		series.addAll(newSeries);
		selected = null;
		update();
	}

	public void addClickListener(Consumer<Point> listener) {
		clickListeners.add(listener);
	}

	/** The point of the series with the given position, if any. */
	public Optional<Point> find(double x, double y) {
		return series.stream().flatMap(s -> s.points().stream())
				.filter(p -> p.x() == x && p.y() == y)
				.findFirst();
	}

	/** Selects the bar of the point (as a click would, but without notifying the listeners). */
	public void select(Point point) {
		selected = point;
		if (isAttached() && point != null) {
			getElement().executeJs("window.fiMsChartSelect(this, $0)", point.id());
		}
	}

	@ClientCallable
	private void onValueClick(String id) {
		series.stream().flatMap(s -> s.points().stream())
				.filter(p -> p.id().equals(id))
				.findFirst()
				.ifPresent(p -> {
					selected = p;
					clickListeners.forEach(l -> l.accept(p));
				});
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		update();
	}

	private void update() {
		if (isAttached()) {
			getElement().executeJs("window.fiMsChart(this, $0, $1)", configJson(), dataJson());
			if (selected != null) {
				select(selected);
			}
		}
	}

	private String configJson() {
		// Settings of the MS and MS/MS details of the Vaadin 8 UI
		return "{\"axisCaptionX\":\"m/z\",\"axisCaptionY\":" + quote(axisCaptionY)
				+ ",\"floatingDigits\":3,\"barStrokeWidth\":3,\"axisOffsetX\":55,\"axisOffsetY\":50"
				+ ",\"axisColor\":\"#474747\",\"selectionColor\":\"#f4bc42\",\"hoverOpacity\":0.9"
				+ ",\"hoverColor\":\"#9c9c9c\",\"textHoverColor\":\"#383838\"}";
	}

	private String dataJson() {
		StringBuilder json = new StringBuilder("{");
		for (Series s : series) {
			if (json.length() > 1) {
				json.append(',');
			}
			json.append(quote(s.id())).append(":{\"id\":").append(quote(s.id()))
					.append(",\"name\":").append(quote(s.name()))
					.append(",\"color\":").append(quote(s.color()))
					.append(",\"points\":[");
			for (int i = 0; i < s.points().size(); i++) {
				Point p = s.points().get(i);
				json.append(i > 0 ? "," : "").append("{\"id\":").append(quote(p.id()))
						.append(String.format(Locale.ROOT, ",\"x\":%s,\"y\":%s}", p.x(), p.y()));
			}
			json.append("]}");
		}
		return json.append('}').toString();
	}

	private static String quote(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
}
