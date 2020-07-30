package edu.caltech.cms.intelliviz.graph.logicalvisualization;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.INode;

import java.util.Collection;
import java.util.Map;

public class GraphStruct {

    public INode root;
    public Map<Long, INode> nodes;
    public Collection<GraphEdge> edges;

    public GraphStruct(INode root, Map<Long, INode> nodes, Collection<GraphEdge> edges) {
        this.root = root;
        this.nodes = nodes;
        this.edges = edges;
    }
}
