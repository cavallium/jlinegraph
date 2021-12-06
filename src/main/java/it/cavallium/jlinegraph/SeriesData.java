package it.cavallium.jlinegraph;

import java.util.List;

public record SeriesData(List<Vertex> vertices, boolean isFunction, String name) {}
