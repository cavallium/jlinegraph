package it.cavallium.jlinegraph;

import java.util.List;

public record SeriesData(List<Vertex> vertices, boolean isFunction, String name, boolean showInLegend) {

	public SeriesData(List<Vertex> vertices, boolean isFunction, String name) {
		this(vertices, isFunction, name, true);
	}

	public SeriesData {
		showInLegend = showInLegend && !name.isBlank();
	}
}
