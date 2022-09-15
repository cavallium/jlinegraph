package it.cavallium.jlinegraph;

public record RasterSize(double width, double height) {
	public static final RasterSize EMPTY = new RasterSize(0, 0);
}
