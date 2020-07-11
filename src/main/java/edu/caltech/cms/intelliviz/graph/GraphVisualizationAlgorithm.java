package edu.caltech.cms.intelliviz.graph;

import java.util.*;
import java.util.function.Predicate;

public class GraphVisualizationAlgorithm {

    public enum LayoutBehavior {
        HORIZONTAL,
        DOUBLY_LINKED,
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

        if (beingHandled.contains(node) || nodesToIgnore.contains(node)) {
            return;
        }

        beingHandled.add(node);
        double last_x = 0, last_y = 0;

        if (node instanceof ClassNode && getChildrenOfSameType((ClassNode)node) >= 2)  {
            String[] prevHeuristics = {"prev", "pre", "last"};
            String[] nextHeuristics = {"next"}; // TODO: think of better things to put here

            Predicate<String> p = s -> node.getChildren().stream().anyMatch(edge -> edge.label.toString().contains(s));
            if (Arrays.stream(prevHeuristics).anyMatch(p) && Arrays.stream(nextHeuristics).anyMatch(p) && getChildrenOfSameType((ClassNode)node) == 2) {
                // drop everything, it's a doubly linked list!
                if (layout != LayoutBehavior.DOUBLY_LINKED) {

                    // if we're in a doubly linked list and are somehow starting from the tail, don't.
                    GraphEdge edge = upstream.getChildren().stream().filter(e -> e.dest.equals(node)).findFirst().get();
                    String[] tailHeuristics = {"tail", "end", "last"};
                    String e = edge.label.toString();
                    if (Arrays.stream(tailHeuristics).anyMatch(s -> e.contains(s))) {
                        return;
                    }

                    // shift down if we haven't already
                    last_y = vSpace;
                    System.out.println("SHIFT IN");
                }
                System.out.println("DOUBLY LINKED!");
                layout = LayoutBehavior.DOUBLY_LINKED;
            } else {
                layout = LayoutBehavior.TREE;
            }
        }

        if (!(upstream instanceof ObjectArrayNode || upstream instanceof ObjectMapNode)) {
            last_x = upstream.getOrigin(null).getX();
            last_y += upstream.getOrigin(null).getY();
        } else if (upstream instanceof ObjectMapNode) {
            last_x = upstream.getX() + upstream.getWidth();
            last_y += upstream.getY();
        } else {
            last_x = upstream.getX(); // this should probably be different, tbh
            last_y += upstream.getY();
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
            } else if (layout == LayoutBehavior.DOUBLY_LINKED) {
                node.setPos(last_x + nodeSpace + upstream.getWidth() / 2, last_y);
            }
        }

        // Handle children
        ArrayList<INode> handleLater = new ArrayList<>();
        ArrayList<GraphEdge> children = node.getChildren();
        children.sort((e1, e2) -> {
            String thisType = declaringTypes.get(node);
            if (e1.declaringType.equals(thisType)) {
                return -1;
            }
            if (e2.declaringType.equals(thisType)) {
                return 1;
            }
            return 0;
        });
        for (GraphEdge downstream : children) {
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
                } else if (layout == LayoutBehavior.DOUBLY_LINKED) {
                    if (declaringTypes.get(node).equals(downstream.declaringType)) {
                        // if the previous node is null, i.e. we're at the head of the list
                        if ((downstream.label.toString().contains("pre") || downstream.label.toString().contains("last"))
                                && downstream.dest instanceof NullNode) {
                            // layout right and move over a bit too
                            downstream.dest.setPos(last_x, last_y);
                            beingHandled.add(downstream.dest);
                            declaringTypes.put(downstream.dest, downstream.declaringType);
                            node.setPos(last_x + downstream.dest.getWidth() + nodeSpace, last_y);
                        } else {
                            layoutNode(downstream, LayoutBehavior.DOUBLY_LINKED, 0);
                        }
                    } else {
                        // force vertical for non-same-type children of doubly linked lists
                        layoutNode(downstream, layout.VERTICAL, bound);
                        bound += getSubgraphWidth(downstream.dest) + nodeSpace;
                    }
                }
            }

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

    }

    private void layoutNode(GraphEdge edge, LayoutBehavior layout, double offset) {
        if (!beingHandled.contains(edge.dest) && !nodesToIgnore.contains(edge.dest)) {
            if (tree.get(edge.source) == null) {
                tree.put(edge.source, new ArrayList<>());
            }
            tree.get(edge.source).add(edge);
            declaringTypes.put(edge.dest, edge.declaringType);
            layoutNode(edge.source, edge.dest, layout, offset);
        }
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
