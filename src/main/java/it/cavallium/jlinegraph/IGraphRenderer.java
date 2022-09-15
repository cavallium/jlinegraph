package it.cavallium.jlinegraph;

public interface IGraphRenderer<T> {

	T renderGraph(Graph graph, GraphBounds bounds);
}
