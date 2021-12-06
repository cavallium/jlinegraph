package it.cavallium.jlinegraph;

public record SeriesStyle(Color color, double pointsWeight, double lineWeight, double areaOpacity, double smoothness) {

	public SeriesStyle {
		if (pointsWeight != 0 && (pointsWeight < 1d || pointsWeight > 4.0d)) {
			throw new IndexOutOfBoundsException();
		}
		if (lineWeight != 0 && (lineWeight < 1d || lineWeight > 4.0d)) {
			throw new IndexOutOfBoundsException();
		}
		if (areaOpacity < 0d || areaOpacity > 1.0d) {
			throw new IndexOutOfBoundsException();
		}
		if (smoothness < 0d || smoothness > 1.0d) {
			throw new IndexOutOfBoundsException();
		}
	}
}
