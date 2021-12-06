package it.cavallium.jlinegraph;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class AWTBufferedGraphRenderer implements IGraphRenderer<BufferedImage> {

	@Override
	public BufferedImage renderGraph(Graph graph, RasterSize totalSize) {
		BufferedImage image = new BufferedImage((int) totalSize.width(),
				(int) totalSize.height(),
				BufferedImage.TYPE_INT_ARGB
		);
		Graphics2D graphics2D = image.createGraphics();
		AWTGraphRenderer.renderGraph(graphics2D, graph, totalSize);
		return image;
	}

}
