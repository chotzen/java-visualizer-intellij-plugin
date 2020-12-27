package edu.caltech.cms.intelliviz.graph;
import edu.caltech.cms.intelliviz.graph.ui.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GraphLayoutAlgorithm {

    public enum LayoutBehavior {
        HORIZONTAL,
        DOUBLY_LINKED,
        VERTICAL,
        TREE,
        COLLAPSE,
        STACKED_UNDERNEATH
    }

    private ArrayList<Node> beingHandled;
    private Set<Node> nodesToIgnore;
    public HashMap<Node, ArrayList<GraphEdge>> tree;
    private HashMap<Node, String> declaringTypes;

    private static final int vSpace = 30;
    private static final int nodeSpace = 70;
    private static final int indent = 40;
    private static final int primSpacing = 5;

    private static final int treeVertSpace = 40;
    private static final int treeHorizSpace = 50;

    private double originX, originY;

    // describe the bounds of the visualization
    private double max_x, max_y;

    GraphLayoutAlgorithm(double originX, double originY, Set<Node> nodesToIgnore) {
        this.originX = originX;
        this.originY = originY;
        this.max_x = originX;
        this.max_y = originY;
        beingHandled = new ArrayList<>();
        declaringTypes = new HashMap<>();
        this.nodesToIgnore = nodesToIgnore;
        tree = new HashMap<>();
    }

    void layoutVariable(VariableNode var)  {

        // center vertically based on dimensions of node immediately downstream
        Node downstream = var.reference;
        double ds_center_y = downstream.getHeight() / 2;

        var.setPos(originX + indent, max_y + vSpace + ds_center_y);

        declaringTypes.put(downstream, var.declaringType);
        if (layoutNode(var, downstream, LayoutBehavior.HORIZONTAL, 0)) {
            this.max_y = getSubgraphMaxY(downstream);
        } else {
            this.max_y = getSubgraphMaxY(var);
        }
    }

    private boolean validateDoublyLinked(Node node, Node traceUntil) {
        if (node.equals(traceUntil)) {
            return false;
        }
        Node curr = node;
        Set<Node> visited = new HashSet<>();
        while (curr.getChildren().stream().anyMatch(edge -> edge.dest instanceof ClassNode && edge.label.label.contains("next")) && !curr.equals(traceUntil)) {
            curr = curr.getChildren().stream().filter(edge -> edge.label.label.contains("next")).map(edge -> edge.dest).findFirst().get();
        }
        return curr.equals(traceUntil);
    }


    private boolean layoutNode(Node upstream, Node node, LayoutBehavior layout, double offset) {

        if (beingHandled.contains(node) || nodesToIgnore.contains(node) || node instanceof StackFrame) {
            return false;
        }

        beingHandled.add(node);
        double last_x = 0, last_y = 0;

        boolean stepIn = false;
        LayoutBehavior prevBehavior = null;
        if ((node instanceof ClassNode || node instanceof HorizontalClassMapNode) && getChildrenOfSameType(node) >= 2)  {
            String[] prevHeuristics = {"prev", "pre", "last"};
            String[] nextHeuristics = {"next"}; // TODO: think of better things to put here
            String[] tailHeuristics = {"tail", "end", "last"};

            Predicate<String> p = s -> node.getChildren().stream().anyMatch(edge -> edge.label.toString().contains(s));

            Optional<GraphEdge> tail = upstream.getChildren().stream().filter(edge -> Arrays.stream(tailHeuristics).anyMatch(h -> h.matches(edge.label.label))).findFirst();
            boolean dllHeuristics = Arrays.stream(prevHeuristics).anyMatch(p) && Arrays.stream(nextHeuristics).anyMatch(p);

            if (dllHeuristics && getChildrenOfSameType(node) == 2 && (tail.isPresent() && layout != LayoutBehavior.DOUBLY_LINKED
                    && !declaringTypes.get(upstream).equals(declaringTypes.get(node)) && validateDoublyLinked(node, tail.get().dest) )) {
                // drop everything, it's a doubly linked list!
                if (layout != LayoutBehavior.DOUBLY_LINKED) {

                    // if we're in a doubly linked list and are somehow starting from the tail, don't.
                    GraphEdge edge = upstream.getChildren().stream().filter(e -> e.dest.equals(node)).findFirst().get();
                    String e = edge.label.toString();
                    if (Arrays.stream(tailHeuristics).anyMatch(s -> e.contains(s))) {
                        return false;
                    }

                    stepIn = true;
                    prevBehavior = layout;
                }
                System.out.println("DOUBLY LINKED!");
                layout = LayoutBehavior.DOUBLY_LINKED;

            } else if (childrenOfSameTypeLeft(node).size() >= 2 && !dllHeuristics) { // this tries to capture the directionality of trees
                if (layout != LayoutBehavior.TREE) {
                    prevBehavior = layout;
                }
                layout = LayoutBehavior.TREE;
            }
        }

        last_x = upstream.getX();
        last_y = upstream.getY();

        if (layout == LayoutBehavior.TREE) {
            if (upstream instanceof VariableNode) {
                last_x = upstream.getX() + upstream.getWidth();
            } else {
                last_x = upstream.getX();
            }
        }

        double bound = 0;

        if (upstream instanceof VariableNode && layout != LayoutBehavior.TREE) { // if it's at the head of the tree
            node.setPos(upstream.getOrigin(null).getX(), upstream.getOrigin(null).getY() - node.getHeight() / 2);
        } else {
            // this assumes that the upstream node is center-originating, which is true for ClassNodes.
            // other functionality to be implemented soon.
            if (layout == LayoutBehavior.VERTICAL) {
                if (upstream instanceof ObjectSetNode) {
                    node.setPos(last_x + offset, last_y + nodeSpace / 2.0 + upstream.getHeight());
                } else {
                    node.setPos(last_x + offset, last_y + nodeSpace + upstream.getHeight());
                }
            } else if (layout == LayoutBehavior.COLLAPSE) {
                node.setPos(last_x + upstream.getWidth() - node.getWidth(), last_y + upstream.getHeight());
                layout = LayoutBehavior.HORIZONTAL;
                if (upstream instanceof ClassNode) {
                    ((ClassNode)upstream).lightMode = true;
                }
            } else if (layout == LayoutBehavior.HORIZONTAL) {
                if (upstream instanceof ObjectMapNode) {
                    node.setPos(last_x + nodeSpace + upstream.getWidth(), upstream.getY() + offset);
                } else {
                    node.setPos(last_x + nodeSpace + upstream.getWidth(), last_y + offset);
                }
            } else if (layout == LayoutBehavior.STACKED_UNDERNEATH) { // same as horizontal but starting from a point
                                                                      // under left side of node
                node.setPos(last_x, upstream.getY() + upstream.getHeight() + offset);
            } else if (layout == LayoutBehavior.TREE) {
                // position is not set from here. it is calculated after the leaves are positioned.
                // this is temporary to pass position data down to the next level
                if (prevBehavior != null) {
                    // layout as we normally would
                    if (upstream instanceof VariableNode) {
                        node.setPos(upstream.getOrigin(null).getX(), upstream.getOrigin(null).getY() - node.getHeight() / 2);
                    } if (prevBehavior == LayoutBehavior.VERTICAL) {
                        node.setPos(last_x + offset, last_y + nodeSpace + upstream.getHeight());
                    } else { // horizontal / doubly linked list? idk
                        last_x = last_x + upstream.getWidth() + nodeSpace; // align to edge of where it "would" be
                        node.setPos(last_x, last_y + offset);
                    }
                } else {
                    node.setPos(last_x, last_y + upstream.getHeight() + treeVertSpace);
                }
            } else if (layout == LayoutBehavior.DOUBLY_LINKED) {
                if (stepIn) {
                    node.setPos(last_x, last_y + upstream.getHeight() + vSpace);
                } else {
                    node.setPos(last_x + nodeSpace + upstream.getWidth(), last_y + upstream.getHeight() / 2 - node.getHeight() / 2);
                }
            }
        }

        // Handle children
        ArrayList<Node> handleLater = new ArrayList<>();
        ArrayList<GraphEdge> handleMuchLater = new ArrayList<>();
        List<GraphEdge> children = node.getChildren();

        if (!(node instanceof ObjectMapNode || node instanceof ObjectArrayNode || node instanceof NullNode || node instanceof ObjectSetNode || node instanceof StackNode)) { // only sort if there's not a prescribed ordering
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
        } else if (node instanceof ObjectArrayNode) {
            children.sort(Comparator.comparing(child -> ((ObjectArrayNode)node).pointers.get(child)));
        }
        if (layout == LayoutBehavior.HORIZONTAL && children.size() > 0) {
            bound += (node.getHeight() - children.get(0).dest.getHeight()) / 2;
        }

        if (bound < 0) {
            bound = 0;
        }

        // collapse layout if only one field &
        if (children.size() == 1 && node instanceof ClassNode && children.get(0).dest instanceof ClassNode && ((ClassNode)node).fields.size() == 0 ) {
            layoutNode(children.get(0), LayoutBehavior.COLLAPSE, bound);
        } else {
            for (GraphEdge downstream : children) {
                if (downstream.label.toString().contains("overallRoot")) {
                    System.out.println("break");
                }
                if (node instanceof ObjectArrayNode) {
                    if (downstream.dest.getWidth() > node.width / 3.0) {
                        layoutNode(downstream, LayoutBehavior.STACKED_UNDERNEATH, bound);
                        bound += getSubgraphHeight(downstream.dest) + vSpace;
                    } else {
                        layoutNode(downstream, LayoutBehavior.VERTICAL, bound);
                        bound += getSubgraphWidth(downstream.dest) + vSpace;
                    }
                } else if (node instanceof ObjectSetNode) {
                    layoutNode(downstream, LayoutBehavior.VERTICAL, bound);
                    bound += getSubgraphWidth(downstream.dest) + vSpace;
                } else if (node instanceof ObjectMapNode) { // force horizontal layout for children of maps
                    layoutNode(downstream, LayoutBehavior.HORIZONTAL, bound);
                    bound += getSubgraphHeight(downstream.dest) + vSpace;
                } else {
                    if (layout == LayoutBehavior.HORIZONTAL) {
                        layoutNode(downstream, layout, bound);
                        bound += getSubgraphHeight(downstream.dest) + vSpace;
                    } else if (layout == LayoutBehavior.VERTICAL) {
                        layoutNode(downstream, layout, bound);
                        bound += getSubgraphWidth(downstream.dest) + vSpace;
                    } else if (layout == LayoutBehavior.TREE) {
                        if (declaringTypes.get(node).equals(downstream.declaringType)) {
                            // this is where subgraphs get calculated. trust the process.
                            // calculates the structure, NOT POSITION of subgraphs
                            layoutNode(downstream, LayoutBehavior.TREE, 0);
                            handleLater.add(downstream.dest);
                        } else {
                            handleMuchLater.add(downstream);
                        }
                    } else if (layout == LayoutBehavior.DOUBLY_LINKED) {
                        if (declaringTypes.get(node).equals(downstream.declaringType)) {
                            // if the previous node is null, i.e. we're at the head of the list
                            if ((downstream.label.toString().contains("pre") || downstream.label.toString().contains("last"))
                                    && downstream.dest instanceof NullNode) {
                                // layout right and move over a bit too
                                downstream.dest.setPos(node.getX(), node.getY() + node.getHeight() / 2 - downstream.dest.getHeight() / 2);
                                beingHandled.add(downstream.dest);
                                declaringTypes.put(downstream.dest, downstream.declaringType);
                                translateSubgraph(node, downstream.dest.getWidth() + nodeSpace, 0);
                                tree.computeIfAbsent(node, k -> new ArrayList<>());
                                tree.get(node).add(downstream);
                            } else {
                                layoutNode(downstream, LayoutBehavior.DOUBLY_LINKED, 0);
                            }
                        } else {
                            // force vertical for non-same-type children of doubly linked lists
                            layoutNode(downstream, LayoutBehavior.VERTICAL, bound);
                            bound += getSubgraphWidth(downstream.dest) + nodeSpace;
                        }
                    }
                }
            }
        }


        if (layout == LayoutBehavior.TREE) {
            double coveredWidth = 0;
            // start at left edge, continuing rightward and adding space in between
            if (node instanceof HorizontalClassMapNode) {
                Collections.reverse(handleLater);
            }
            for (Node child : handleLater) {
                translateSubgraph(child, coveredWidth, 0);
                if (!(child instanceof StackFrame)) {
                    coveredWidth += getSubgraphWidth(child) + treeHorizSpace;
                }
            }
            coveredWidth -= treeHorizSpace;
            if (handleLater.size() >= 2) {
                node.setPos(last_x + coveredWidth / 2 - node.getWidth() / 2, node.getY());
            }

            for (GraphEdge ds : handleMuchLater) {
                layoutNode(ds, LayoutBehavior.HORIZONTAL, bound);
                bound += getSubgraphHeight(ds.dest) + nodeSpace;
            }
        }
        return true;
    }

    private boolean layoutNode(GraphEdge edge, LayoutBehavior layout, double offset) {
        if (!beingHandled.contains(edge.dest) && !nodesToIgnore.contains(edge.dest)) {
            tree.computeIfAbsent(edge.source, k -> new ArrayList<>());
            tree.get(edge.source).add(edge);
            declaringTypes.put(edge.dest, edge.declaringType);
            layoutNode(edge.source, edge.dest, layout, offset);
            return true;
        }
        return false;
    }


    private void translateSubgraph(Node head, double dx, double dy) {
        head.setPos(head.getX() + dx, head.getY() + dy);
        if (tree.containsKey(head)) {
            tree.get(head).forEach(edge -> translateSubgraph(edge.dest, dx, dy));
        }
    }

    private int getChildrenOfSameType(Node node) {
        // Checks nodes immediately downstream to see if they're the same type
        return (int)node.getChildren().stream()
                // want to only search down the tree
                .filter(edge -> edge.declaringType.equals(declaringTypes.get(node))
                                /*&& !(edge.dest instanceof StackFrame)*/)
                .count();
    }

    private List<Node> childrenOfSameTypeLeft(Node node) {
        return node.getChildren().stream()
                .filter(edge -> edge.declaringType.equals(declaringTypes.get(node)))
                .filter(edge -> !beingHandled.contains(edge.dest))
                .map(edge -> edge.dest)
                .collect(Collectors.toList());
    }

    private double getSubgraphHeight(Node node) {
        return getSubgraphMaxY(node) - getSubgraphMinY(node);
    }

    private double getSubgraphMaxY(Node node) {
        double max_y = node.getY() + node.getHeight();
        if (tree.containsKey(node)) {
            for (GraphEdge edge : tree.get(node)) {
                max_y = Math.max(max_y, getSubgraphMaxY(edge.dest));
            }
        }
        return max_y;
    }

    private double getSubgraphMinY(Node node) {
        double min_y = node.getY();
        if (tree.containsKey(node)) {
            for (GraphEdge edge : tree.get(node)) {
                min_y = Math.min(min_y, getSubgraphMinY(edge.dest));
            }
        }
        return min_y;
    }

    private double getSubgraphWidth(Node node) {
        if (node instanceof StackFrame) {
            throw new IllegalArgumentException("Cannot get width of subgraph involving stackframes!");
        }
        double max_x = getSubgraphMaxX(node);
        this.max_x = Math.max(max_x, this.max_x);
        return max_x - getSubgraphMinX(node);
    }

    private double getSubgraphMaxX(Node node) {
        double max_x = node.getX() + node.getWidth();
        if (tree.containsKey(node)) {
            for (GraphEdge edge : tree.get(node)) {
                max_x = Math.max(max_x, getSubgraphMaxX(edge.dest));
            }
        }
        return max_x;
    }

    private double getSubgraphMinX(Node node) {
        double min_x = node.getX();
        if (tree.containsKey(node)) {
            for (GraphEdge edge : tree.get(node)) {
                min_x = Math.min(min_x, getSubgraphMinX(edge.dest));
            }
        }
        return min_x;
    }


    double getMaxY() {
        return this.max_y;
    }

    double getMaxX() {
        return this.max_x;
    }
}
