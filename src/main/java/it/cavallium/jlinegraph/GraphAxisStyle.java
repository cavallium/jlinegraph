package it.cavallium.jlinegraph;

import java.util.function.Function;

public record GraphAxisStyle(String title, boolean showName, AxisMode mode, Function<Number, String> valueFormat) {}
