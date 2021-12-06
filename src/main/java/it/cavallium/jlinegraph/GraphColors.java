package it.cavallium.jlinegraph;

public record GraphColors(Color background, Color foreground) {

	public static GraphColors DARK = new GraphColors(new Color(0.1f, 0.1f, 0.1f, 1f), new Color(0.9f, 0.9f, 0.9f, 1f));
	public static GraphColors LIGHT = new GraphColors(new Color(1.0f, 1.0f, 1.0f, 1f), new Color(0.0f, 0.0f, 0.0f, 1f));
}
