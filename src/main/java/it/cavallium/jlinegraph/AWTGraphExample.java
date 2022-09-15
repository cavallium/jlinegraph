package it.cavallium.jlinegraph;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class AWTGraphExample {

	private static final GraphColors GRAPH_COLOR = GraphColors.DARK;

	public static void main(String[] args) {
		var jf = new JFrame("Graph");
		jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		jf.setLocationByPlatform(true);
		var jp = new JComponent() {
			@Override
			protected void paintComponent(Graphics g) {
				generateGraph(this.getWidth(), this.getHeight(), (Graphics2D) g);
			}
		};
		jf.setBackground(GRAPH_COLOR.background().toColor());
		jf.add(jp);
		jf.setPreferredSize(new Dimension(800, 600));
		jf.pack();
		jf.setVisible(true);
	}

	private static void generateGraph(int w, int h, Graphics2D g2d) {
		var g = new Graph("Example", new GraphData(List.of(
				new SeriesData(List.of(
						new Vertex(-3, -3),
						new Vertex(0, 0),
						new Vertex(3, 1),
						new Vertex(3.5, 7),
						new Vertex(7, 0.5),
						new Vertex(15, 14)
				), true, "Data1"),
				new SeriesData(List.of(
						new Vertex(-3, -2),
						new Vertex(0, 3),
						new Vertex(1, 1),
						new Vertex(2, 5),
						new Vertex(3, 6),
						new Vertex(4, 7),
						new Vertex(5, 4),
						new Vertex(6, 3),
						new Vertex(7, 1),
						new Vertex(8, 10),
						new Vertex(9, 12),
						new Vertex(10, 13),
						new Vertex(11, 15),
						new Vertex(12, 11),
						new Vertex(13, 15),
						new Vertex(14, 10),
						new Vertex(15, 4)
				), true, "Data2"),
				new SeriesData(List.of(
						new Vertex(-3, -1),
						new Vertex(0, 3),
						new Vertex(4, 4),
						new Vertex(8, 3),
						new Vertex(12, 4),
						new Vertex(15, 3)
				), true, "Data3"),
				new SeriesData(List.of(
						new Vertex(4.4, 5.2),
						new Vertex(6.4, 7.2),
						new Vertex(8.4, 5.2),
						new Vertex(6.4, 3.2),
						new Vertex(4.4, 5.2)
				), false, "full oval"),
				new SeriesData(List.of(
						new Vertex(-6+4.4, 5.8),
						new Vertex(-6+6.4, 7.8),
						new Vertex(-6+8.4, 5.8),
						new Vertex(-6+6.4, 3.8),
						new Vertex(-6+4.4, 5.8)
				), false, "oval line"),
				new SeriesData(List.of(
						new Vertex(3.3+4, 2.3+8),
						new Vertex(3.3+5, 2.3+7),
						new Vertex(3.3+8, 2.3+5),
						new Vertex(3.3+6, 2.3+3)
				), false, "open path")
		)),
				new GraphStyle(List.of(
						new SeriesStyle(new Color(0f, 1f, 0f, 1f), 1, 1, 0, 1d),
						new SeriesStyle(new Color(1f, 0f, 0f, 1f), 1, 0, 0, 1d),
						new SeriesStyle(new Color(0.5f, 1f, 1f, 1f), 0, 1, 0.3, 1d),
						new SeriesStyle(new Color(0.5f, 1f, 0.5f, 1f), 0, 0, 1, 1d),
						new SeriesStyle(new Color(0.5f, 1f, 0.5f, 1f), 0, 1, 0.3, 1d),
						new SeriesStyle(new Color(1f, 1f, 0.7f, 1f), 1.5, 2, 0, 1d)
				),
						new GraphAxisStyle("X axis", true, AxisMode.SHOW_WITH_VALUES, "%.2fs"::formatted),
						new GraphAxisStyle("Y axis", true, AxisMode.SHOW_WITH_VALUES, "%.2fm"::formatted),
						GRAPH_COLOR,
						new GraphFonts(10f, 18f, 12f, 12f),
						2f,
						true,
								1
				));
		var r = new AWTGraphRenderer();
		r.renderGraph(g, new GraphBounds(0, 0, w, h)).drawTo(g2d);
	}
}
