package it.cavallium.jlinegraph;

import java.util.List;

public record GraphStyle(List<SeriesStyle> seriesStyles, GraphAxisStyle x, GraphAxisStyle y, GraphColors colors,
												 GraphFonts fonts, double strokeWidth, boolean showLegend, double paddingMultiplier) {}
