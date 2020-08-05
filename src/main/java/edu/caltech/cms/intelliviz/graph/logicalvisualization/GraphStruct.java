package edu.caltech.cms.intelliviz.graph.logicalvisualization;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;

import java.util.Collection;
import java.util.Map;

public class GraphStruct {

    public Node root;
    public Map<Long, Node> nodes;
    public Collection<GraphEdge> edges;

    public GraphStruct(Node root, Map<Long, Node> nodes, Collection<GraphEdge> edges) {
        this.root = root;
        this.nodes = nodes;
        this.edges = edges;
    }
}
