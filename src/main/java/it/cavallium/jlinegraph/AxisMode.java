package it.cavallium.jlinegraph;

public enum AxisMode {
	SHOW_WITH_VALUES,
	SHOW_RULER_ONLY,
	HIDE;

	public boolean showLabels() {
		return switch (this) {
			case SHOW_WITH_VALUES -> true;
			default -> false;
		};
	}

	public boolean showRuler() {
		return switch (this) {
			case SHOW_WITH_VALUES, SHOW_RULER_ONLY -> true;
			default -> false;
		};
	}
}
