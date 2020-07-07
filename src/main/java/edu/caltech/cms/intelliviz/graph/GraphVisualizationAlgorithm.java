package edu.caltech.cms.intelliviz.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class GraphVisualizationAlgorithm {

    public enum LayoutBehavior {
        HORIZONTAL,
        VERTICAL
    }

    private ArrayList<INode> beingHandled;
    private Set<INode> nodesToIgnore;
    public HashMap<INode, ArrayList<GraphEdge>> tree;
    private static final int vSpace = 30;
    private static final int nodeSpace = 50;
    private static final int indent = 40;
    private static final int primSpacing = 5;

    private double originX, originY;

    // describe the bounds of the visualization
    private double max_x, max_y;

    /*
    Splitting algorithm:
    - assign children of "this" but not those which are downstream from local variables
    - do the vertical offset to the stackframes
     */

    public GraphVisualizationAlgorithm(double originX, double originY, Set<INode> nodesToIgnore) {
        this.originX = originX;
        this.originY = originY;
        this.max_x = originX;
        this.max_y = originY;
        beingHandled = new ArrayList<>();
        this.nodesToIgnore = nodesToIgnore;
        tree = new HashMap<>();
    }

    public void layoutVariable(VariableNode var)  {

        // center vertically based on dimensions of node immediately downstream
        INode downstream = var.reference;
        double ds_center_y = downstream.getHeight() / 2;

        var.setPos(originX + indent, max_y + vSpace + ds_center_y);

        layoutNode(var, downstream, LayoutBehavior.HORIZONTAL, 0);
        this.max_y = getSubgraphMaxY(downstream);
    }


    void layoutNode(INode upstream, INode node, LayoutBehavior layout, double offset) {
        double last_x, last_y;
        if (!(upstream instanceof ObjectArrayNode || upstream instanceof ObjectMapNode)) {
            last_x = upstream.getOrigin(null).getX();
            last_y = upstream.getOrigin(null).getY();
        } else if (upstream instanceof ObjectMapNode) {
            last_x = upstream.getX() + upstream.getHeight();
            last_y = upstream.getY();
        } else {
            last_x = upstream.getX(); // this should probably be different, tbh
            last_y = upstream.getY();
        }

        double bound = 0;

        if (upstream instanceof VariableNode) { // if it's at the head of the tree
            if (node instanceof PrimitiveNode) {
                node.setPos(last_x + primSpacing, last_y - node.getHeight() / 2); // render next to it, but not as much
            } else {
                node.setPos(last_x + vSpace, last_y - node.getHeight() / 2); // render next to it, but more
            }
        } else {
            // this assumes that the upstream node is center-originating, which is true for ClassNodes.
            // other functionality to be implemented soon.
            if (layout == LayoutBehavior.VERTICAL) {
                System.out.println("offset " + offset);
                node.setPos(last_x + offset, last_y + nodeSpace + upstream.getHeight() / 2);
            } else { // horizontal
                System.out.println(last_x + " " + (nodeSpace + upstream.getWidth() / 2));
                node.setPos(last_x + nodeSpace + upstream.getWidth() / 2, last_y  + offset);
            }
        }

        // Handle children
        tree.put(node, new ArrayList<>());
        for (GraphEdge downstream : node.getChildren()) {
            System.out.println(downstream.label.toString());
            if (!beingHandled.contains(downstream.dest) && !nodesToIgnore.contains(downstream.dest)) {
                tree.get(node).add(downstream);
                if (node instanceof ObjectArrayNode) { // force vertical layout for children of arrays
                    layoutNode(downstream, LayoutBehavior.VERTICAL, bound);
                    bound += getSubgraphWidth(downstream.dest) + nodeSpace;
                } else if (node instanceof ObjectMapNode) { // force horizontal layout for children of maps
                    layoutNode(downstream, LayoutBehavior.HORIZONTAL, bound);
                    bound += getSubgraphHeight(downstream.dest) + nodeSpace;
                } else {
                    layoutNode(downstream, layout, bound);
                    if (layout == LayoutBehavior.HORIZONTAL) {
                        bound += getSubgraphHeight(downstream.dest) + nodeSpace;
                    } else {
                        bound += getSubgraphWidth(downstream.dest) + nodeSpace;
                    }
                }
            }
        }

        beingHandled.add(node);
    }

    void layoutNode(GraphEdge edge, LayoutBehavior layout, double offset) {
        layoutNode(edge.source, edge.dest, layout, offset);
    }

    private double getSubgraphHeight(INode node) {
        return getSubgraphMaxY(node) - getSubgraphMinY(node);
    }

    private double getSubgraphMaxY(INode node) {
        double max_y = node.getY() + node.getHeight();
        for (GraphEdge edge : node.getChildren()) {
            max_y = Math.max(max_y, getSubgraphMaxY(edge.dest));
        }
        return max_y;
    }

    private double getSubgraphMinY(INode node) {
        double min_y = node.getY();
        for (GraphEdge edge : node.getChildren()) {
            min_y = Math.min(min_y, getSubgraphMinY(edge.dest));
        }
        return min_y;
    }
    private double getSubgraphWidth(INode node) {
        return getSubgraphMaxX(node) - getSubgraphMinX(node);
    }

    private double getSubgraphMaxX(INode node) {
        double max_x = node.getX() + node.getWidth();
        for (GraphEdge edge : node.getChildren()) {
            max_x = Math.max(max_x, getSubgraphMaxX(edge.dest));
        }
        return max_x;
    }

    private double getSubgraphMinX(INode node) {
        double min_x = node.getX();
        for (GraphEdge edge : node.getChildren()) {
            min_x = Math.min(min_x, getSubgraphMinX(edge.dest));
        }
        return min_x;
    }


    public double getMax_y() {
        return this.max_y;
    }
}
