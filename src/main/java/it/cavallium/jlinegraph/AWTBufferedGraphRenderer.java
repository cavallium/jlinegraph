package it.cavallium.jlinegraph;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class AWTBufferedGraphRenderer implements IGraphRenderer<BufferedImage> {

	@Override
	public BufferedImage renderGraph(Graph graph, GraphBounds bounds) {
		BufferedImage image = new BufferedImage((int) bounds.maxX(),
				(int) bounds.maxY(),
				BufferedImage.TYPE_INT_ARGB
		);
		Graphics2D graphics2D = image.createGraphics();
		AWTGraphRenderer.renderGraph(graphics2D, graph, bounds);
		return image;
	}

}
