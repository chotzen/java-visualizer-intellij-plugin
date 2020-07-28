package edu.caltech.cms.intelliviz.graph.logical_visualizations;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.INode;

import java.util.List;
import java.util.Map;

public class GraphStruct {
    public INode root;
    public Map<Long, INode> nodes;
    public List<GraphEdge> edges;

    public GraphStruct(INode root, Map<Long, INode> nodes, List<GraphEdge> edges) {
        this.root = root;
        this.nodes = nodes;
        this.edges = edges;
    }
}
