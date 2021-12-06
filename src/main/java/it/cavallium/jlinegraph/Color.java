package it.cavallium.jlinegraph;

public record Color(float red, float green, float blue, float alpha) {

	public Color {
		if (red < 0d || red > 1.0d) {
			throw new IndexOutOfBoundsException();
		}
		if (green < 0d || green > 1.0d) {
			throw new IndexOutOfBoundsException();
		}
		if (blue < 0d || blue > 1.0d) {
			throw new IndexOutOfBoundsException();
		}
		if (alpha < 0d || alpha > 1.0d) {
			throw new IndexOutOfBoundsException();
		}
	}

	public static Color fromRGB(int rgb) {
		var col = new java.awt.Color(rgb);
		return new Color(col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f, 1);
	}

	public java.awt.Color toColor() {
		return new java.awt.Color(red, green, blue, alpha);
	}

	public Color multiplyOpacity(float alpha) {
		return new Color(red, green, blue, this.alpha * alpha);
	}

	public Color overrideOpacity(float alpha) {
		return new Color(red, green, blue, alpha);
	}
}
