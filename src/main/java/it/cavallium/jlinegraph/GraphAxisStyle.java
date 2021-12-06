package it.cavallium.jlinegraph;

import java.util.function.Function;

public record GraphAxisStyle(String title, boolean show, Function<Number, String> valueFormat) {}
