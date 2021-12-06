package it.cavallium.jlinegraph;

import java.util.List;

public record GraphData(List<SeriesData> series, GraphBounds bounds) {
	
	public GraphData(List<SeriesData> series) {
		this(series, GraphBounds.fromSeriesData(series, false, false));
	}
}
