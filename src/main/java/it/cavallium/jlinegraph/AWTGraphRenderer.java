package it.cavallium.jlinegraph;

import it.cavallium.jlinegraph.AWTGraphRenderer.AWTDrawer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class AWTGraphRenderer implements IGraphRenderer<AWTDrawer> {

	private static final int MAX_LABELS = 1000;

	@Override
	public AWTDrawer renderGraph(Graph graph, GraphBounds bounds) {
		return graphics2D -> renderGraph(graphics2D, graph, bounds);
	}

	public static void renderGraph(Graphics2D g2d, Graph graph, GraphBounds bounds) {
		var graphics2D = (Graphics2D) g2d.create();
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
		graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		graphics2D.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		graphics2D.setRenderingHint(RenderingHints.KEY_RESOLUTION_VARIANT, RenderingHints.VALUE_RESOLUTION_VARIANT_DPI_FIT);

		Font defaultFont = graphics2D.getFont().deriveFont((float) graph.style().fonts().global());
		Font valuesFont = defaultFont.deriveFont((float) graph.style().fonts().valueLabel());
		Font axisNameFont = defaultFont.deriveFont((float) graph.style().fonts().axisName());

		var defaultFontMetrics = graphics2D.getFontMetrics(defaultFont);
		var valuesFontMetrics = graphics2D.getFontMetrics(valuesFont);
		var axisNameFontMetrics = graphics2D.getFontMetrics(axisNameFont);

		var paddingMultiplier = graph.style().paddingMultiplier();
		var graphBounds = graph.data().bounds();
		var x = graph.style().x();
		var y = graph.style().y();
		var padding = defaultFontMetrics.getHeight() * paddingMultiplier;
		var scaleX = new NiceScale(graphBounds.minX(), graphBounds.maxX());
		scaleX.setMaxTicks(20);
		var scaleY = new NiceScale(graphBounds.minY(), graphBounds.maxY());
		scaleY.setMaxTicks(20);
		var halfMaxXLabelWidth = x.mode().showLabels()
						? (valuesFontMetrics.stringWidth(y.valueFormat().apply(graphBounds.maxX())) / 2d) : 0;
		var halfMaxYLabelHeight = (y.mode().showLabels() ? valuesFontMetrics.getHeight() / 2d : 0);
		var topPadding = padding + halfMaxYLabelHeight;
		var leftPadding = padding
						+ ((y.mode() == AxisMode.HIDE && !y.showName()) ? halfMaxXLabelWidth : 0);
		var rightPadding = padding + (x.mode().showLabels() ? halfMaxXLabelWidth : 0);
		var bottomPadding = padding
						+ ((x.mode() == AxisMode.HIDE && !x.showName()) ? halfMaxYLabelHeight : 0);
		var xValueLineLength = x.mode().showRuler() ? valuesFontMetrics.getHeight() : 0;
		var yValueLineLength = y.mode().showRuler() ? valuesFontMetrics.getHeight() : 0;
		var xValuesHeight = x.mode().showLabels() ? valuesFontMetrics.getHeight() : 0;
		var xValuesToXAxisNamePadding = (x.showName() ? valuesFontMetrics.getHeight() / 2 : 0);
		var xAxisNameHeight = (x.showName() ? axisNameFontMetrics.getHeight() : 0);
		var yAxisNameWidth = (y.showName() ? axisNameFontMetrics.getHeight() : 0);
		var yValuesToYAxisNamePadding = (y.showName() ? valuesFontMetrics.getHeight() / 2 : 0);

		var graphHeight
				// Start with total height
				= bounds.height()
				// Remove the padding on top
				- topPadding
				// Remove the x value lines length
				- xValueLineLength
				// Remove the values height
				- xValuesHeight
				// Remove the space between the values and the axis name
				- xValuesToXAxisNamePadding
				// Remove x-axis name height
				- xAxisNameHeight
				// Remove the padding on bottom
				- bottomPadding;

		var xValueLineOffset = bounds.minY() + topPadding + graphHeight;

		var yLabels = getYLabels(graph, bounds.minY(), graphHeight, valuesFontMetrics, scaleY, y.mode());
		RasterSize yLabelsAreaSize = computeYLabelsAreaSize(y.mode(), graphHeight, valuesFontMetrics, yLabels);
		var yValuesWidth = yLabelsAreaSize.width();
		var yValueLineOffset = bounds.minX() + leftPadding + yAxisNameWidth + yValuesToYAxisNamePadding + yValuesWidth;

		var graphWidth
				// Start with total width
				= bounds.width()
				// Remove the padding on left
				- leftPadding
				// Remove y-axis name "90deg height"
				- yAxisNameWidth
				// Remove the space between the values and the axis name
				- yValuesToYAxisNamePadding
				// Remove the y values width
				- yValuesWidth
				// Remove the y value lines length
				- yValueLineLength
				// Remove the padding on right
				- rightPadding;

		Font seriesNameFont = null;
		FontMetrics seriesNameFontMetrics = null;

		if (graph.style().showLegend()) {
			double legendSizeW;
			double legendSizeH;

			seriesNameFont = defaultFont.deriveFont((float) graph.style().fonts().seriesName());
			seriesNameFontMetrics = graphics2D.getFontMetrics(seriesNameFont);
			legendSizeW = getLegendSizeW(graph, seriesNameFontMetrics);
			legendSizeH = getLegendSizeH(graph, seriesNameFontMetrics);

			if (legendSizeW > graphWidth / 3d || legendSizeH > graphHeight / 2.5d) {
				var newFontSizeW = (float) (seriesNameFont.getSize() * ((graphWidth / 3d) / legendSizeW));
				var newFontSizeH = (float) (seriesNameFont.getSize() * ((graphHeight / 2.5d) / legendSizeH));
				seriesNameFont = seriesNameFont.deriveFont(Math.min(newFontSizeW, newFontSizeH));
				seriesNameFontMetrics = graphics2D.getFontMetrics(seriesNameFont);
			}
		}

		var xLabels = getXLabels(graph, bounds.minX(), graphWidth, valuesFontMetrics, scaleX, x.mode());

		RasterSize yAxisNameCenterOffset = new RasterSize(bounds.minX()
				+ leftPadding
				+ valuesFontMetrics.getHeight() / 2d, bounds.minY()
				+ valuesFontMetrics.getHeight()
				// Add half of graph height
				+ graphHeight / 2 + topPadding);

		RasterSize yValuesOffset = new RasterSize(bounds.minX()
				+ leftPadding
				// Add y axis name "90deg height"
				+ yAxisNameWidth
				// Add the space between the values and the axis name
				+ yValuesToYAxisNamePadding, bounds.minY() + topPadding);

		RasterSize graphOffset = new RasterSize(bounds.minX()
				+ leftPadding
				+ yAxisNameWidth
				+ yValuesToYAxisNamePadding
				+ yValuesWidth
				+ yValueLineLength, bounds.minY() + topPadding);
		RasterSize xValuesOffset = new RasterSize(graphOffset.width(), xValueLineOffset + xValueLineLength);

		RasterSize xAxisNameCenterOffset = new RasterSize(bounds.minX()
				+	leftPadding
				+ yAxisNameWidth
				+ yValuesToYAxisNamePadding
				+ yValuesWidth
				+ yValueLineLength
				// Add half of graph width
				+ graphWidth / 2, bounds.minY()
				+ topPadding
				// Add graph height
				+ graphHeight
				// Add the x value lines length
				+ xValueLineLength
				// Add the x values height
				+ xValuesHeight
				// Add the space between the values and the axis name
				+ xValuesToXAxisNamePadding
				// Add x-axis half name height
				+ axisNameFontMetrics.getHeight() / 2d);

		RasterSize graphSize = new RasterSize(graphWidth, graphHeight);

		var bgColor = graph.style().colors().background().toColor();
		var fgColor = graph.style().colors().foreground().toColor();
		var strokeWidth = graph.style().strokeWidth();
		var defaultStroke = new BasicStroke((float) strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_MITER);

		try {
			graphics2D.setBackground(bgColor);
			graphics2D.clearRect((int) Math.floor(bounds.minX()),
							(int) Math.floor(bounds.minY()),
							(int) Math.ceil(bounds.width()),
							(int) Math.ceil(bounds.height())
			);

			if (graphHeight < 0) {
				return;
			}
			if (graphWidth < 0) {
				return;
			}

			renderGraphBorders(graphics2D, graph, graphOffset, graphSize, defaultStroke, bounds);
			if (y.showName()) {
				renderYAxisName(graphics2D, graph, yAxisNameCenterOffset, axisNameFont, axisNameFontMetrics);
			}
			if (x.showName()) {
				renderXAxisName(graphics2D, graph, xAxisNameCenterOffset, axisNameFont, axisNameFontMetrics);
			}
			renderYAxisValueLabels(graphics2D,
					graph,
					valuesFont,
					valuesFontMetrics,
					yValueLineOffset,
					yValueLineLength,
					yLabels,
					yLabelsAreaSize,
					yValuesOffset,
					defaultStroke,
					y.mode().showRuler(),
					y.mode().showLabels()
			);
			renderXAxisValueLabels(graphics2D,
					graph,
					valuesFont,
					valuesFontMetrics,
					xValueLineOffset,
					xValueLineLength,
					xLabels, xValuesOffset,
					defaultStroke,
					x.mode().showRuler(),
					x.mode().showLabels()
			);


			var seriesGraphics2D = (Graphics2D) graphics2D.create();
			seriesGraphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			try {
				seriesGraphics2D.setClip(new Rectangle2D.Double(graphOffset.width() - defaultStroke.getLineWidth(),
						graphOffset.height() - defaultStroke.getLineWidth(),
						graphSize.width() + defaultStroke.getLineWidth() * 2d,
						graphSize.height() + defaultStroke.getLineWidth() * 2d
				));

				var zeroLineStroke = new BasicStroke((float) strokeWidth,
						BasicStroke.CAP_ROUND,
						BasicStroke.JOIN_ROUND,
						10.0f,
						new float[]{2.0f, 3.0f},
						0.0f
				);
				var zeroLineColor = graph.style().colors().foreground().multiplyOpacity(0.5f).toColor();

				if ((graphBounds.minY() < 0 && graphBounds.maxY() > 0)
						|| (graphBounds.minY() > 0 && graphBounds.maxY() < 0)) {
					double rasterZeroY = graphOffset.height() + graphSize.height()
							- ((-graphBounds.minY()) / (graphBounds.maxY() - graphBounds.minY())) * graphSize.height();
					seriesGraphics2D.setColor(zeroLineColor);
					seriesGraphics2D.setStroke(zeroLineStroke);
					seriesGraphics2D.draw(new Line2D.Double(graphOffset.width(),
							rasterZeroY,
							graphOffset.width() + graphWidth,
							rasterZeroY
					));
				}
				if ((graphBounds.minX() < 0 && graphBounds.maxX() > 0)
						|| (graphBounds.minX() > 0 && graphBounds.maxX() < 0)) {
					double rasterZeroX = graphOffset.width()
							+ ((-graphBounds.minX()) / (graphBounds.maxX() - graphBounds.minX())) * graphSize.width();
					seriesGraphics2D.setColor(zeroLineColor);
					seriesGraphics2D.setStroke(zeroLineStroke);
					seriesGraphics2D.draw(new Line2D.Double(
							rasterZeroX,
							graphOffset.height(),
							rasterZeroX,
							graphOffset.height() + graphHeight
					));
				}

				int i = 0;
				for (SeriesData series : graph.data().series()) {
					var seriesStyleSize = graph.style().seriesStyles().size();
					if (graph.style().seriesStyles().isEmpty()) {
						throw new IllegalArgumentException("No styles found");
					}
					SeriesStyle style = graph.style().seriesStyles().get(i % seriesStyleSize);
					BasicStroke seriesStroke = getSeriesStroke(style, strokeWidth);
					BasicStroke seriesPointsStroke = getSeriesPointsStroke(style, strokeWidth);
					drawSeries(seriesGraphics2D, graphBounds,
							graphOffset,
							graphSize,
							series,
							style,
							seriesStroke,
							seriesPointsStroke
					);
					i++;
				}
			} finally {
				seriesGraphics2D.dispose();
			}

			if (graph.style().showLegend()) {
				drawSeriesLegend(graphics2D,
						graph,
						graphOffset,
						graphSize,
						seriesNameFont,
						seriesNameFontMetrics,
						defaultStroke,
						fgColor,
						strokeWidth
				);
			}
		} finally {
			graphics2D.dispose();
		}
	}

	private static void drawSeriesLegend(Graphics2D graphics2D,
			Graph graph,
			RasterSize graphOffset,
			RasterSize graphSize,
			Font seriesNameFont,
			FontMetrics seriesNameFontMetrics,
			BasicStroke defaultStroke,
			Color fgColor,
			double strokeWidth) {
		double seriesPadding = getSeriesPadding(seriesNameFontMetrics);
		double seriesMargin = getSeriesMargin(seriesNameFontMetrics);
		double seriesPreviewLineWidth = seriesNameFontMetrics.getHeight() * 2;
		double singleSeriesHeight = seriesNameFontMetrics.getHeight();

		double legendSizeW = getLegendSizeW(graph, seriesNameFontMetrics);
		double legendSizeH = getLegendSizeH(graph, seriesNameFontMetrics);

		double legendOffsetX = graphOffset.width()
				+ graphSize.width()
				- seriesMargin
				- legendSizeW;
		double legendOffsetY = graphOffset.height()
				+ seriesMargin;

		var legendRect = new Rectangle2D.Double(legendOffsetX, legendOffsetY, legendSizeW, legendSizeH);

		graphics2D.setStroke(defaultStroke);

		graphics2D.setColor(graph.style().colors().background().multiplyOpacity(0.75f).toColor());
		graphics2D.fill(legendRect);
		graphics2D.setColor(fgColor);
		graphics2D.draw(legendRect);

		int i = 0;
		for (SeriesData series : graph.data().series()) {
			var seriesStyleSize = graph.style().seriesStyles().size();
			if (graph.style().seriesStyles().isEmpty()) {
				throw new IllegalArgumentException("No styles found");
			}
			SeriesStyle style = graph.style().seriesStyles().get(i % seriesStyleSize);

			var seriesName = series.name();
			var stroke = new BasicStroke((float) (strokeWidth * 2f), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
			graphics2D.setColor(style.color().overrideOpacity(1.0f).toColor());
			graphics2D.setStroke(stroke);
			var lineOffsetX = legendOffsetX + seriesPadding;
			var currentOffsetY = legendOffsetY + seriesPadding / 2d
					+ i * (seriesPadding / 2d + singleSeriesHeight + seriesPadding / 2d)
					+ seriesPadding / 2d;
			var lineOffsetY = currentOffsetY + singleSeriesHeight / 2d;
			graphics2D.draw(new Line2D.Double(lineOffsetX, lineOffsetY, lineOffsetX + seriesPreviewLineWidth, lineOffsetY));
			var textOffsetX = lineOffsetX + seriesPreviewLineWidth + seriesPadding;
			var textOffsetY = currentOffsetY + seriesNameFontMetrics.getAscent();
			graphics2D.setColor(fgColor);
			graphics2D.setFont(seriesNameFont);
			graphics2D.fill(generateShapeFromText(graphics2D, seriesName, textOffsetX, textOffsetY));
			i++;
		}
	}

	private static double getSeriesMargin(FontMetrics seriesNameFontMetrics) {
		return seriesNameFontMetrics.getHeight() * 2d / 3d;
	}

	private static double getLegendSizeW(Graph graph, FontMetrics seriesNameFontMetrics) {
		double seriesPadding = getSeriesPadding(seriesNameFontMetrics);
		double seriesTextMaxWidth = getSeriesTextMaxWidth(graph, seriesNameFontMetrics);
		double seriesPreviewLineWidth = seriesNameFontMetrics.getHeight() * 2;
		return seriesPadding
				+ seriesTextMaxWidth
				+ seriesPadding
				+ seriesPreviewLineWidth
				+ seriesPadding;
	}

	private static double getSeriesPadding(FontMetrics seriesNameFontMetrics) {
		return seriesNameFontMetrics.getHeight() / 3d;
	}

	private static double getLegendSizeH(Graph graph, FontMetrics seriesNameFontMetrics) {
		int seriesCount = graph.data().series().size();
		double seriesPadding = getSeriesPadding(seriesNameFontMetrics);
		double singleSeriesHeight = seriesNameFontMetrics.getHeight();
		return  seriesPadding / 2d
				+ seriesCount * (seriesPadding / 2d + singleSeriesHeight + seriesPadding / 2d)
				+ seriesPadding / 2d;
	}

	private static double getSeriesTextMaxWidth(Graph graph, FontMetrics seriesNameFontMetrics) {
		double seriesTextMaxWidth = 0;
		for (SeriesData series : graph.data().series()) {
			var seriesName = series.name();
			var seriesNameRasterWidth = seriesNameFontMetrics.stringWidth(seriesName);
			if (seriesTextMaxWidth < seriesNameRasterWidth) {
				seriesTextMaxWidth = seriesNameRasterWidth;
			}
		}
		return seriesTextMaxWidth;
	}

	private static void drawSeries(Graphics2D seriesGraphics2D,
			GraphBounds graphBounds,
			RasterSize graphOffset,
			RasterSize graphSize,
			SeriesData series,
			SeriesStyle style,
			BasicStroke seriesStroke,
			BasicStroke seriesPointsStroke) {
		var lineColor = style.color().toColor();
		var areaColor = style.color().multiplyOpacity((float) style.areaOpacity()).toColor();
		var points = new java.awt.geom.Point2D.Double[series.vertices().size()];

		double rasterMinY = graphOffset.height() + graphSize.height()
				- ((0 - graphBounds.minY()) / (graphBounds.maxY() - graphBounds.minY())) * graphSize.height();
		int i = 0;
		for (Vertex vertex : series.vertices()) {
			double rasterX = graphOffset.width()
					+ ((vertex.x() - graphBounds.minX()) / (graphBounds.maxX() - graphBounds.minX())) * graphSize.width();
			double rasterY = graphOffset.height() + graphSize.height()
					- ((vertex.y() - graphBounds.minY()) / (graphBounds.maxY() - graphBounds.minY())) * graphSize.height();
			points[i] = new java.awt.geom.Point2D.Double(rasterX, rasterY);
			i++;
		}
		// Sort points if it's a function
		if (series.isFunction()) {
			Arrays.sort(points, Comparator.comparingDouble(Point2D.Double::getX));
		}

		seriesGraphics2D.setStroke(seriesPointsStroke);
		if (style.pointsWeight() != 0) {
			seriesGraphics2D.setColor(lineColor);
			for (var point : points) {
				seriesGraphics2D.fill(new Ellipse2D.Double(point.getX() - seriesPointsStroke.getLineWidth(),
						point.getY() - seriesPointsStroke.getLineWidth(),
						seriesPointsStroke.getLineWidth() * 2f,
						seriesPointsStroke.getLineWidth() * 2f
				) {});
			}
		}
		if (style.lineWeight() != 0 || style.areaOpacity() > 0d) {
			if (style.smoothness() > 0d && points.length >= 3) {
				var mPath = new GeneralPath(Path2D.WIND_NON_ZERO, points.length);
				var areaPath = new GeneralPath(Path2D.WIND_NON_ZERO, points.length);

				if (series.isFunction()) {
					areaPath.moveTo(points[0].x, rasterMinY);
					mPath.moveTo(points[0].x, points[0].y);
					areaPath.lineTo(points[0].x, points[0].y);
				} else {
					areaPath.moveTo(points[0].x, points[0].y);
					mPath.moveTo(points[0].x, points[0].y);
				}

				double SMOOTHNESS = style.smoothness() / 2d; // higher is smoother, but don't go over 0.5

				if (!series.isFunction()) {
					Point2D.Double[] bezierPoints;
					final boolean closedPath = Objects.equals(points[points.length - 1], points[0]);
					if (closedPath) {
						bezierPoints = new Point2D.Double[points.length + 2];
						bezierPoints[0] = points[points.length - 2];
						System.arraycopy(points, 0, bezierPoints, 1, points.length);
						bezierPoints[bezierPoints.length - 1] = points[1];
					} else {
						bezierPoints = points;
					}
					var bez = new Bezier(bezierPoints);
					Point2D[] b = bez.getPoints();

					if (!closedPath) {
						mPath.quadTo(b[0].getX(), b[0].getY(), bezierPoints[1].x, bezierPoints[1].getY());
						areaPath.quadTo(b[0].getX(), b[0].getY(), bezierPoints[1].x, bezierPoints[1].getY());
					}

					for (int w = 2; w < bezierPoints.length - 1; w++) {
						Point2D b0 = b[2 * w - 3];
						Point2D b1 = b[2 * w - 2];
						double cp1X = b0.getX();
						double cp1Y =  b0.getY();
						double cp2X = b1.getX();
						double cp2Y = b1.getY();
						double endPointX = bezierPoints[w].getX();
						double endPointY = bezierPoints[w].getY();
						mPath.curveTo(cp1X, cp1Y, cp2X, cp2Y, endPointX, endPointY);
						areaPath.curveTo(cp1X, cp1Y, cp2X, cp2Y, endPointX, endPointY);
					}

					if (!closedPath) {
						mPath.quadTo(b[b.length - 1].getX(),
								b[b.length - 1].getY(),
								bezierPoints[bezierPoints.length - 1].x,
								bezierPoints[bezierPoints.length - 1].getY()
						);
						areaPath.quadTo(b[b.length - 1].getX(),
								b[b.length - 1].getY(),
								bezierPoints[bezierPoints.length - 1].x,
								bezierPoints[bezierPoints.length - 1].getY()
						);
					}
				} else {
					// calculate smooth path
					double lX = 0, lY = 0;
					int size = points.length;
					for (int pointIndex=1; pointIndex<size; pointIndex++) {
						java.awt.geom.Point2D.Double p = points[pointIndex];	// current point

						// first control point
						var p0 = points[pointIndex-1];	// previous point
						double d0 = Math.sqrt(Math.pow(p.x - p0.x, 2)+Math.pow(p.y-p0.y, 2));	// distance between p and p0
						double x1 = Math.min(p0.x + lX*d0, (p0.x + p.x)/2); 	// min is used to avoid going too much right
						double y1 = p0.y + lY*d0;

						// second control point
						var p1 = points[pointIndex+1 < size ? pointIndex+1 : pointIndex];	// next point
						double d1 = Math.sqrt(Math.pow(p1.x - p0.x, 2)+Math.pow(p1.y-p0.y, 2));	// distance between p1 and p0 (length of reference line)
						lX = (p1.x-p0.x)/d1*SMOOTHNESS;		// (lX,lY) is the slope of the reference line
						lY = (p1.y-p0.y)/d1*SMOOTHNESS;
						double x2 = Math.max(p.x - lX*d0, (p0.x + p.x)/2);	// max is used to avoid going too much left
						double y2 = p.y - lY*d0;

						// add line
						mPath.curveTo(x1,y1,x2, y2, p.x, p.y);
						areaPath.curveTo(x1, y1, x2, y2, p.x, p.y);
					}
				}

				if (series.isFunction()) {
					areaPath.lineTo(points[points.length - 1].x, rasterMinY);
				}
				areaPath.closePath();
				if (style.areaOpacity() > 0d) {
					seriesGraphics2D.setColor(areaColor);
					seriesGraphics2D.fill(areaPath);
				}
				if (style.lineWeight() != 0) {
					seriesGraphics2D.setStroke(seriesStroke);
					seriesGraphics2D.setColor(lineColor);
					seriesGraphics2D.draw(mPath);
				}
			} else {
				var areaPath = new Path2D.Double();
				var path = new Path2D.Double();
				if (points.length > 0) {
					areaPath.moveTo(points[0].x, rasterMinY);
				}
				boolean first = true;
				for (Point2D.Double point : points) {
					if (first) {
						path.moveTo(point.getX(), point.getY());
						first = false;
					} else {
						path.lineTo(point.getX(), point.getY());
					}
					areaPath.lineTo(point.getX(), point.getY());
				}
				if (points.length > 0) {
					areaPath.moveTo(points[points.length - 1].x, rasterMinY);
					areaPath.closePath();
				}
				if (style.areaOpacity() > 0d) {
					seriesGraphics2D.setStroke(seriesStroke);
					seriesGraphics2D.setColor(areaColor);
					seriesGraphics2D.fill(areaPath);
				}
				if (style.lineWeight() != 0) {
					seriesGraphics2D.setStroke(seriesStroke);
					seriesGraphics2D.setColor(lineColor);
					seriesGraphics2D.draw(path);
				}
			}
		}
	}

	private static BasicStroke getSeriesStroke(SeriesStyle seriesStyle, double defaultStrokeWidth) {
		return new BasicStroke((float) (defaultStrokeWidth * seriesStyle.lineWeight()),
				BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND
		);
	}

	private static BasicStroke getSeriesPointsStroke(SeriesStyle seriesStyle, double defaultStrokeWidth) {
		return new BasicStroke((float) (defaultStrokeWidth * 2d * seriesStyle.pointsWeight()),
				BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND
		);
	}

	private static void renderYAxisName(Graphics2D graphics2D,
			Graph graph,
			RasterSize yAxisNameCenterOffset,
			Font axisNameFont,
			FontMetrics axisNameFontMetrics) {
		var fgColor = graph.style().colors().foreground().toColor();
		graphics2D.setColor(fgColor);
		graphics2D.setFont(axisNameFont);

		var title = graph.style().y().title();

		var previousTransform = graphics2D.getTransform();
		graphics2D.rotate(Math.toRadians(-90),
				yAxisNameCenterOffset.width(),
				yAxisNameCenterOffset.height()
		);
		graphics2D.fill(generateShapeFromText(graphics2D,
				title,
				yAxisNameCenterOffset.width() - axisNameFontMetrics.stringWidth(title) / 2d,
				yAxisNameCenterOffset.height() + axisNameFontMetrics.getHeight() / 2d - axisNameFontMetrics.getDescent()
		));
		graphics2D.setTransform(previousTransform);
	}

	private static void renderYAxisValueLabels(Graphics2D graphics2D,
			Graph graph,
			Font valuesFont,
			FontMetrics valuesFontMetrics,
			double yValueLineOffset,
			int yValueLineLength,
			List<LabelWithOffset> yLabels,
			RasterSize yLabelsAreaSize,
			RasterSize yValuesOffset,
			BasicStroke defaultStroke,
			boolean showRulerTicks,
			boolean showRulerLabels) {
		if ((yValueLineLength > 0 && showRulerTicks) || showRulerLabels) {
			graphics2D.setFont(valuesFont);
			graphics2D.setStroke(defaultStroke);
			graphics2D.setColor(graph.style().colors().foreground().toColor());
			yLabels.forEach(label -> {
				if (showRulerTicks) {
					var lineStartOffsetY = yValuesOffset.height() + label.rasterOffset();
					var currentLineOffsetX = label.formattedText().isBlank() ? yValueLineLength / 3d : 0;
					var currentLineLength = yValueLineLength + (label.formattedText().isBlank() ? -yValueLineLength / 3d : 0);
					graphics2D.draw(new Line2D.Double(yValueLineOffset + currentLineOffsetX,
						lineStartOffsetY,
						yValueLineOffset + currentLineOffsetX + currentLineLength,
						lineStartOffsetY
					));
				}
				if (showRulerLabels) {
					graphics2D.fill(generateShapeFromText(graphics2D,
						label.formattedText(),
						yValuesOffset.width() + yLabelsAreaSize.width() - valuesFontMetrics.stringWidth(label.formattedText()),
						yValuesOffset.height() + label.rasterOffset() + valuesFontMetrics.getHeight() / 2d - valuesFontMetrics.getDescent()
					));
				}
			});
		}
	}

	private static void renderXAxisName(Graphics2D graphics2D,
			Graph graph,
			RasterSize xAxisNameCenterOffset,
			Font axisNameFont,
			FontMetrics axisNameFontMetrics) {
		var fgColor = graph.style().colors().foreground().toColor();
		graphics2D.setColor(fgColor);
		graphics2D.setFont(axisNameFont);

		var title = graph.style().x().title();

		graphics2D.fill(generateShapeFromText(graphics2D,
				title,
				xAxisNameCenterOffset.width() - axisNameFontMetrics.stringWidth(title) / 2d,
				xAxisNameCenterOffset.height() + axisNameFontMetrics.getHeight() / 2d - axisNameFontMetrics.getDescent()
		));
	}

	private static void renderXAxisValueLabels(Graphics2D graphics2D,
			Graph graph,
			Font valuesFont,
			FontMetrics valuesFontMetrics,
			double xValueLineOffset,
			int xValueLineLength,
			List<LabelWithOffset> xLabels,
			RasterSize xValuesOffset,
			BasicStroke defaultStroke,
			boolean showRulerTicks,
			boolean showRulerLabels) {
		if ((xValueLineLength > 0 && showRulerTicks) || showRulerLabels) {
			graphics2D.setFont(valuesFont);
			graphics2D.setStroke(defaultStroke);
			graphics2D.setColor(graph.style().colors().foreground().toColor());
			xLabels.forEach(label -> {
				var lineStartOffsetX = xValuesOffset.width() + label.rasterOffset();
				if (showRulerTicks) {
					var currentLineLength = label.formattedText().isBlank() ? xValueLineLength / 1.5d : xValueLineLength;
					//noinspection SuspiciousNameCombination
					graphics2D.draw(new Line2D.Double(lineStartOffsetX, xValueLineOffset, lineStartOffsetX, xValueLineOffset + currentLineLength));
				}
				if (showRulerLabels) {
					graphics2D.fill(generateShapeFromText(graphics2D,
						label.formattedText(),
						xValuesOffset.width() + label.rasterOffset() - valuesFontMetrics.stringWidth(label.formattedText()) / 2d,
						xValuesOffset.height() + valuesFontMetrics.getHeight()
					));
				}
			});
		}
	}

	private static void renderGraphBorders(Graphics2D graphics2D,
			Graph graph,
			RasterSize graphOffset,
			RasterSize graphSize,
			BasicStroke defaultStroke,
			GraphBounds bounds) {
		// Do not draw the border if the graph is fullscreen
		if (graphOffset.width() <= bounds.minX()
						&& graphOffset.height() <= bounds.minY()
						&& graphSize.width() >= bounds.width()
						&& graphSize.height() >= bounds.height()) {
			return;
		}
		var fgColor = graph.style().colors().foreground().toColor();
		graphics2D.setColor(fgColor);
		graphics2D.setStroke(defaultStroke);
		graphics2D.draw(new Rectangle2D.Double(graphOffset.width(),
				graphOffset.height(),
				graphSize.width(),
				graphSize.height()
		));
	}

	private static RasterSize computeYLabelsAreaSize(AxisMode axisMode, double graphHeight, FontMetrics valuesFontMetrics,
					List<LabelWithOffset> yLabels) {
		if (!axisMode.showLabels()) {
			return RasterSize.EMPTY;
		}
		double maxLabelWidth = 0d;
		for (LabelWithOffset yLabel : yLabels) {
			var currentMaxLabelWidth = valuesFontMetrics.stringWidth(yLabel.formattedText);
			if (currentMaxLabelWidth > maxLabelWidth) {
				maxLabelWidth = currentMaxLabelWidth;
			}
		}

		return new RasterSize(maxLabelWidth, graphHeight);
	}

	record LabelWithOffset(double value, double rasterOffset, String formattedText) {}

	/**
	 * @return rendered labels
	 */
	private static List<LabelWithOffset> getXLabels(Graph graph,
			double labelsAreaOffset,
			double labelsAreaWidth,
			FontMetrics valuesFontMetrics,
			NiceScale scaleX,
			AxisMode mode) {
		if (mode == AxisMode.HIDE) {
			return List.of();
		}
		var bounds = graph.data().bounds();
		var minX = bounds.minX();
		var maxX = bounds.maxX();
		var format = graph.style().x().valueFormat();
		double singleRasterOffset = labelsAreaWidth / ((maxX - minX) / scaleX.getTickSpacing());

		ArrayList<LabelWithOffset> labels = new ArrayList<>();

		int i = 0;
		double prevRasterLabelEndOffset = -Double.MAX_VALUE;
		double currentRasterOffset = labelsAreaOffset;
		double currentValue = minX;
		while (currentValue <= maxX && i < MAX_LABELS && (scaleX.getTickSpacing() > 0)) {
			if (mode.showLabels()) {
				var formatted = format.apply(currentValue);
				var stringWidth = valuesFontMetrics.stringWidth(formatted);
				if (currentRasterOffset - stringWidth / 2d > prevRasterLabelEndOffset) {
					labels.add(new LabelWithOffset(currentValue, currentRasterOffset, formatted));
					prevRasterLabelEndOffset = currentRasterOffset + stringWidth / 2d;
				} else {
					labels.add(new LabelWithOffset(currentValue, currentRasterOffset, ""));
				}
			} else {
				labels.add(new LabelWithOffset(currentValue, currentRasterOffset, ""));
			}

			i++;
			currentValue = minX + i * scaleX.getTickSpacing();
			currentRasterOffset = i * singleRasterOffset;
		}
		return labels;
	}

	/**
	 * @return rendered labels
	 */
	private static List<LabelWithOffset> getYLabels(Graph graph,
			double labelsAreaOffset,
			double labelsAreaHeight,
			FontMetrics valuesFontMetrics,
			NiceScale scaleY,
			AxisMode mode) {
		if (mode == AxisMode.HIDE) {
			return List.of();
		}
		var bounds = graph.data().bounds();
		var minY = bounds.minY();
		var maxY = bounds.maxY();
		var format = graph.style().y().valueFormat();
		double singleRasterOffset = labelsAreaHeight / ((maxY - minY) / scaleY.getTickSpacing());
		double stringTop = valuesFontMetrics.getAscent();
		double stringBottom = valuesFontMetrics.getDescent();

		ArrayList<LabelWithOffset> labels = new ArrayList<>();

		int i = 0;
		double prevRasterLabelEndOffset = Double.MAX_VALUE;
		double currentRasterOffset = labelsAreaHeight;
		double currentValue = minY;
		while (currentValue <= maxY && i < MAX_LABELS && (scaleY.getTickSpacing() > 0)) {
			if (mode.showLabels()) {
				if (currentRasterOffset + stringBottom < prevRasterLabelEndOffset) {
					labels.add(new LabelWithOffset(currentValue, currentRasterOffset, format.apply(currentValue)));
					prevRasterLabelEndOffset = currentRasterOffset - stringTop;
				} else {
					labels.add(new LabelWithOffset(currentValue, currentRasterOffset, ""));
				}
			} else {
				labels.add(new LabelWithOffset(currentValue, currentRasterOffset, ""));
			}

			i++;
			currentValue = minY + i * scaleY.getTickSpacing();
			currentRasterOffset = labelsAreaHeight - i * singleRasterOffset;
		}

		return labels;
	}

	public static Shape generateShapeFromText(Graphics2D graphics2D, String string, double x, double y) {
		GlyphVector vector = graphics2D.getFont().createGlyphVector(graphics2D.getFontRenderContext(), string);
		return vector.getOutline((float) x, (float) y);// - (float) vector.getVisualBounds().getY());
	}

	public interface AWTDrawer {

		void drawTo(Graphics2D graphics2D);
	}
}
