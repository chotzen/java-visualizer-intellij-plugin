package edu.caltech.cms.intelliviz.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class GraphVisualizationAlgorithm {

    public enum LayoutBehavior {
        HORIZONTAL,
        VERTICAL,
        TREE
    }

    private ArrayList<INode> beingHandled;
    private Set<INode> nodesToIgnore;
    public HashMap<INode, ArrayList<GraphEdge>> tree;
    private HashMap<INode, String> declaringTypes;
    private static final int vSpace = 30;
    private static final int nodeSpace = 50;
    private static final int indent = 40;
    private static final int primSpacing = 5;

    private static final int treeVertSpace = 40;
    private static final int treeHorizSpace = 50;

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
        declaringTypes = new HashMap<>();
        this.nodesToIgnore = nodesToIgnore;
        tree = new HashMap<>();
    }

    public void layoutVariable(VariableNode var)  {

        // center vertically based on dimensions of node immediately downstream
        INode downstream = var.reference;
        double ds_center_y = downstream.getHeight() / 2;

        var.setPos(originX + indent, max_y + vSpace + ds_center_y);

        declaringTypes.put(downstream, var.declaringType);
        layoutNode(var, downstream, LayoutBehavior.HORIZONTAL, 0);
        this.max_y = getSubgraphMaxY(downstream);
    }


    void layoutNode(INode upstream, INode node, LayoutBehavior layout, double offset) {
        System.out.println("huqwodjk");
        double last_x, last_y;
        // TODO: make this more sophisticated
        if (node instanceof ClassNode && getChildrenOfSameType((ClassNode)node) >= 2)  {
            layout = LayoutBehavior.TREE;
        }
        if (!(upstream instanceof ObjectArrayNode || upstream instanceof ObjectMapNode)) {
            last_x = upstream.getOrigin(null).getX();
            last_y = upstream.getOrigin(null).getY();
        } else if (upstream instanceof ObjectMapNode) {
            last_x = upstream.getX() + upstream.getWidth();
            last_y = upstream.getY();
        } else {
            last_x = upstream.getX(); // this should probably be different, tbh
            last_y = upstream.getY();
        }

        if (layout == LayoutBehavior.TREE) {
            last_x = upstream.getX();
            last_y = upstream.getY() + upstream.getHeight();
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
                node.setPos(last_x + offset, last_y + nodeSpace + upstream.getHeight() / 2);
            } else if (layout == LayoutBehavior.HORIZONTAL) {
                node.setPos(last_x + nodeSpace + upstream.getWidth() / 2, last_y + offset);
            } else if (layout == LayoutBehavior.TREE) {
                // position is not set from here. it is calculated after the leaves are positioned.
                // this is temporary to pass position data down to the next level
                node.setPos(last_x, last_y + treeVertSpace);
            }
        }

        // Handle children
        tree.put(node, new ArrayList<>());
        ArrayList<INode> handleLater = new ArrayList<>();
        for (GraphEdge downstream : node.getChildren()) {
            if (!beingHandled.contains(downstream.dest) && !nodesToIgnore.contains(downstream.dest)) {
                tree.get(node).add(downstream);
                if (node instanceof ObjectArrayNode) { // force vertical layout for children of arrays
                    layoutNode(downstream, LayoutBehavior.VERTICAL, bound);
                    bound += getSubgraphWidth(downstream.dest) + nodeSpace;
                } else if (node instanceof ObjectMapNode) { // force horizontal layout for children of maps
                    layoutNode(downstream, LayoutBehavior.HORIZONTAL, bound);
                    bound += getSubgraphHeight(downstream.dest) + nodeSpace;
                } else {
                    if (layout == LayoutBehavior.HORIZONTAL) {
                        layoutNode(downstream, layout, bound);
                        bound += getSubgraphHeight(downstream.dest) + nodeSpace;
                    } else if (layout == LayoutBehavior.VERTICAL){
                        layoutNode(downstream, layout, bound);
                        bound += getSubgraphWidth(downstream.dest) + nodeSpace;
                    } else if (layout == LayoutBehavior.TREE) {
                        if (declaringTypes.get(node).equals(downstream.declaringType)) {
                            // this is where subgraphs get calculated. trust the process.
                            // calculates the structure, NOT POSITION of subgraphs
                            layoutNode(downstream, LayoutBehavior.TREE, 0);
                            handleLater.add(downstream.dest);
                        } else {
                            layoutNode(downstream, LayoutBehavior.HORIZONTAL, bound);
                            bound += getSubgraphHeight(downstream.dest) + nodeSpace;
                        }
                    }
                }

            }
        }
        boolean debug = false;
        if (handleLater.stream().filter(k -> k instanceof NullNode).count() >= 2) {
            debug = true;
            System.out.println("breakpoint me!");
        }

        if (layout == LayoutBehavior.TREE) {
            System.out.println("TREE LAYOUT!!!");
            double coveredWidth = 0;
            // start at left edge, continuing rightward and adding space in between
            for (INode child : handleLater) {
                translateSubgraph(child, coveredWidth, 0);
                coveredWidth += getSubgraphWidth(child) + treeHorizSpace;
            }
            coveredWidth -= treeHorizSpace;
            if (handleLater.size() >= 2) {
                node.setPos(last_x + coveredWidth / 2 - node.getWidth() / 2, node.getY());
            }
        }
        System.out.println("HERE");

        beingHandled.add(node);
    }

    private void translateSubgraph(INode head, double dx, double dy) {
        head.setPos(head.getX() + dx, head.getY() + dy);
        if (tree.containsKey(head)) {
            tree.get(head).forEach(edge -> translateSubgraph(edge.dest, dx, dy));
        }
    }

    private int getChildrenOfSameType(ClassNode node) {
        // Checks nodes immediately downstream to see if they're the same type
        return (int)node.getChildren().stream()
                // want to only search down the tree
                .filter(edge -> edge.declaringType.equals(declaringTypes.get(node)))
                .count();
    }

    private void layoutNode(GraphEdge edge, LayoutBehavior layout, double offset) {
        if (!beingHandled.contains(edge.dest) && !nodesToIgnore.contains(edge.dest)) {
            declaringTypes.put(edge.dest, edge.declaringType);
        }
        layoutNode(edge.source, edge.dest, layout, offset);
    }

    private double getSubgraphHeight(INode node) {
        return getSubgraphMaxY(node) - getSubgraphMinY(node);
    }

    private double getSubgraphMaxY(INode node) {
        double max_y = node.getY() + node.getHeight();
        if (tree.containsKey(node)) {
            for (GraphEdge edge : tree.get(node)) {
                max_y = Math.max(max_y, getSubgraphMaxY(edge.dest));
            }
        }
        return max_y;
    }

    private double getSubgraphMinY(INode node) {
        double min_y = node.getY();
        if (tree.containsKey(node)) {
            for (GraphEdge edge : tree.get(node)) {
                min_y = Math.min(min_y, getSubgraphMinY(edge.dest));
            }
        }
        return min_y;
    }

    private double getSubgraphWidth(INode node) {
        return getSubgraphMaxX(node) - getSubgraphMinX(node);
    }

    private double getSubgraphMaxX(INode node) {
        double max_x = node.getX() + node.getWidth();
        if (tree.containsKey(node)) {
            for (GraphEdge edge : tree.get(node)) {
                max_x = Math.max(max_x, getSubgraphMaxX(edge.dest));
            }
        }
        return max_x;
    }

    private double getSubgraphMinX(INode node) {
        double min_x = node.getX();
        if (tree.containsKey(node)) {
            for (GraphEdge edge : tree.get(node)) {
                min_x = Math.min(min_x, getSubgraphMinX(edge.dest));
            }
        }
        return min_x;
    }


    public double getMax_y() {
        return this.max_y;
    }
}
