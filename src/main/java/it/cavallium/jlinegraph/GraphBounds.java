package it.cavallium.jlinegraph;

import java.util.List;

public record GraphBounds(double minX, double minY, double maxX, double maxY) {

	private static final GraphBounds EMPTY = new GraphBounds(0, 0, 0, 0);

	public static GraphBounds fromSeriesData(List<SeriesData> seriesDataList,
			boolean includeOriginX,
			boolean includeOriginY) {
		var merged = merge(seriesDataList
				.stream()
				.map(bound -> fromSeriesData(bound, includeOriginX, includeOriginY))
				.toList());
		return adjustZero(merged, includeOriginX, includeOriginY);
	}

	private static GraphBounds adjustZero(GraphBounds bounds, boolean showZeroX, boolean showZeroY) {
		double minX = bounds.minX();
		double minY = bounds.minY();
		double maxX = bounds.maxX();
		double maxY = bounds.maxY();
		if (showZeroY) {
			minY = Math.min(0, bounds.minY());
			maxY = Math.max(0, bounds.maxY());
		}
		if (showZeroX) {
			minX = Math.min(0, bounds.minX());
			maxX = Math.max(0, bounds.maxX());
		}
		return new GraphBounds(minX, minY, maxX, maxY);
	}

	public static GraphBounds merge(List<GraphBounds> list) {
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		boolean empty = true;
		for (GraphBounds graphBounds : list) {
			if (empty) {
				empty = false;
			}
			if (minX > graphBounds.minX()) {
				minX = graphBounds.minX();
			}
			if (maxX < graphBounds.maxX()) {
				maxX = graphBounds.maxX();
			}
			if (minY > graphBounds.minY()) {
				minY = graphBounds.minY();
			}
			if (maxY < graphBounds.maxY()) {
				maxY = graphBounds.maxY();
			}
		}
		if (empty) {
			return EMPTY;
		} else {
			return new GraphBounds(minX, minY, maxX, maxY);
		}
	}

	public static GraphBounds fromSeriesData(SeriesData seriesData, boolean showZeroX, boolean showZeroY) {
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		boolean empty = true;
		for (Vertex vertex : seriesData.vertices()) {
			if (empty) {
				empty = false;
			}
			if (minX > vertex.x()) {
				minX = vertex.x();
			}
			if (maxX < vertex.x()) {
				maxX = vertex.x();
			}
			if (minY > vertex.y()) {
				minY = vertex.y();
			}
			if (maxY < vertex.y()) {
				maxY = vertex.y();
			}
		}
		if (empty) {
			return EMPTY;
		} else {
			return adjustZero(new GraphBounds(minX, minY, maxX, maxY), showZeroX, showZeroY);
		}
	}
}
