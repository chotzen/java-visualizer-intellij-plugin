package edu.caltech.cms.intelliviz.graph;

import com.aegamesi.java_visualizer.model.*;
import com.aegamesi.java_visualizer.model.Frame;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.LogicalVisualization;
import edu.caltech.cms.intelliviz.graph.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GraphCanvas extends JPanel {

    private double scale = 1.0;

    public JScrollPane parent = null;

    private Map<Frame, StackFrame> frameMap;
    private LinkedHashMap<StackFrame, List<VariableNode>> variables;
    private List<VariableNode> otherVariables = new ArrayList<>();
    private Map<Long, Node> nodes;
    private Map<Long, Node> lastNodes;
    private Map<Long, VariableNode> locals;
    private List<GraphEdge> edges;
    private Map<Node, List<GraphEdge>> layoutTree;

    private Set<Node> selected;
    private Cursor curCursor;

    private ExecutionTrace trace;

    private Graphics grRef;

    private double x1, y1;
    private boolean init = false;

    static final NotificationGroup VIZ_NOTIFICATIONS = new NotificationGroup("Java Visualization",
            NotificationDisplayType.TOOL_WINDOW, true);

    public GraphCanvas() {
        super();
        this.grRef = getGraphics();

        this.variables = new LinkedHashMap<>();
        this.nodes = new ConcurrentHashMap<>();
        this.edges = new ArrayList<>();
        this.frameMap = new HashMap<>();
        this.locals = new HashMap<>();

        LogicalVisualization.loadFromCfg();

        setBackground(Color.WHITE);
        setVisible(true);
        addMouseListener(new MyMouseListener());
        addMouseMotionListener(new MyMouseMotionListener());
    }

    public void setTrace(ExecutionTrace t) {
        this.trace = t;
        refreshUI();
    }

    public void setScale(double scale) {
        this.scale = scale;
        if (this.trace != null) {
            refreshUI();
        }
    }

    public void refreshUI() {
        //nodes.clear();
        edges.clear();
        variables.clear();
        otherVariables.clear();

        removeAll();

        buildUI();

        revalidate();
        repaint();
    }

    public VariableNode buildVariable(Frame fr, String v, Map<Long, VariableNode> oldLocals, boolean isStatic) {
        Value val = isStatic ? fr.statics.get(v) : fr.locals.get(v);
        if (val.type == Value.Type.REFERENCE) {
            if (!v.equals("this")) {
                VariableNode var = new VariableNode(0, 0, v, renderNode(trace.heap.get(val.reference)), val.referenceType);
                return var;
            }
        } else if (val.type == Value.Type.HOLE) {
            StackFrame dest = this.frameMap.get(val.holeDest);
            CodeNode cn = new CodeNode(0, 0, val.holeString);
            cn.holeDest = dest;
            VariableNode var = new VariableNode(0, 0, v, cn, val.type.toString());
            this.nodes.put(getUniqueNegKey(), cn);
            dest.targeted = true;
            return var;
        } else if (val.type == Value.Type.CODE) {
            CodeNode dest = new CodeNode(0, 0, val.codeValue);
            VariableNode var = new VariableNode(0, 0, v, dest, val.type.toString());
            this.nodes.put(getUniqueNegKey(), dest);
            return var;
        } else {
            PrimitiveNode node = new PrimitiveNode(0, 0, val.toString());
            VariableNode var = new VariableNode(0, 0, v, node, val.type.toString());
            this.nodes.put(getUniqueNegKey(), node);
            this.locals.put(val.hashCode, var);

            if (oldLocals.containsKey(val.hashCode)) {
                node.highlightChanges(oldLocals.get(val.hashCode).reference);
            }
            return var;
        }
        return null;
    }

    public void buildUI() {
        Map<Long, VariableNode> oldLocals = this.locals;
        this.locals = new ConcurrentHashMap<>();
        this.lastNodes = this.nodes;
        this.nodes = new ConcurrentHashMap<>();

        int depth = 0;
        VariableNode thisNode = null;
        Collections.reverse(this.trace.frames);

        Map<StackFrame, Frame> invFrameMap = new HashMap<>();
        // Initialize stack frames
        for (Frame fr : this.trace.frames) {
            StackFrame convert = new StackFrame(this.trace.heap, fr, depth,
                    !fr.equals(this.trace.frames.get(this.trace.frames.size() - 1)));
            this.variables.put(convert, new ArrayList<>());
            frameMap.put(fr, convert);
            invFrameMap.put(convert, fr);
            depth++;
        }

        // Initialize variables, by frame
        // this is done separately to allow hole pointers to work
        for (Frame fr : this.variables.keySet().stream().filter(invFrameMap::containsKey)
                                                        .map(invFrameMap::get)
                                                        .collect(Collectors.toSet())) {
            StackFrame convert = frameMap.get(fr);
            for (String v : fr.locals.keySet()) {
                this.variables.get(convert).add(buildVariable(fr, v, oldLocals, false));
            }
        }

        Frame last = this.trace.frames.get(this.trace.frames.size() - 1);
        StackFrame conv = frameMap.get(last);
        if (last.locals.containsKey("this")) {
            VariableNode var = new VariableNode(0, 0, "this", renderNode(trace.heap.get(last.locals.get("this").reference)), last.locals.get("this").referenceType);
            this.variables.get(conv).add(var);
            thisNode = var;
        }

        for (String key : last.statics.keySet()) {
            VariableNode vNode = buildVariable(last, key, oldLocals, true);
            otherVariables.add(vNode);
            this.variables.get(conv).add(vNode);
        }

        init = false;

        Set<Node> allDownstream = new HashSet<>();
        Map<StackFrame, Set<Node>> downstreams = new HashMap<>();

        List<Map.Entry<StackFrame, List<VariableNode>>> list = new ArrayList<>(this.variables.entrySet());
        Collections.reverse(list);

        for (Map.Entry<StackFrame, List<VariableNode>> ent : list) {
            // Map each stackframe to downstream nodes that have not yet been searched.
            // this works because LinkedHashMap iterates based on insertion order, and
            // this.variables is a LinkedHashMap, probably.
            VariableNode finalThisNode1 = thisNode;
            downstreams.put(ent.getKey(), new HashSet<>(
                    ent.getValue().stream()
                            .filter(node -> node != null && !node.name.equals("this"))
                            .flatMap(vNode -> findDownstreamNodes(vNode.reference).stream())
                            .filter(node -> !allDownstream.contains(node) && !(finalThisNode1 != null && node.equals(finalThisNode1.reference)))
                            .collect(Collectors.toSet())
            ));

            allDownstream.addAll(downstreams.get(ent.getKey()));
        }

        this.repaint();

        VariableNode finalThisNode = thisNode;
        SwingUtilities.invokeLater(() -> {

            /*
            new plan:
            - render `this` without allDownstream
            - remove the next batch of nodes from allDownstream
            - render them.
             */

            double spacing = 20;

            HashMap<Node, List<GraphEdge>> tree = new HashMap<>();
            double vertOffset = spacing;
            double horizOffset = 0;
            if (finalThisNode != null || otherVariables.size() != 0) {
                GraphLayoutAlgorithm upperLayout = new GraphLayoutAlgorithm(20, 20, allDownstream);
                if (finalThisNode != null)
                    upperLayout.layoutVariable(finalThisNode);
                for (VariableNode v : otherVariables) {
                    allDownstream.remove(v.reference);
                    upperLayout.layoutVariable(v);
                }
                tree.putAll(upperLayout.tree);
                vertOffset = upperLayout.getMaxY() + spacing;
            }

            for (Map.Entry<StackFrame, List<VariableNode>> ent : this.variables.entrySet()) {
                allDownstream.removeAll(downstreams.get(ent.getKey()));

                ent.getKey().vertOffset = (int) vertOffset;
                System.out.println(vertOffset);
                Point2D origin = ent.getKey().getOrigin();
                GraphLayoutAlgorithm lowerLayout = new GraphLayoutAlgorithm(origin.getX(), origin.getY(), allDownstream);

                // sort variables to put holes last.
                ent.getValue().sort((v1, v2) -> {
                    if (v1 == null) {
                        return -1;
                    }
                    if (v2 == null) {
                        return 1;
                    }
                    if (v1.reference instanceof StackFrame) {
                        return 1;
                    }
                    if (v2.reference instanceof StackFrame) {
                        return -1;
                    }
                    return 0;
                });

                for (VariableNode v: ent.getValue()) {
                    if (v != null && !v.equals(finalThisNode) && !otherVariables.contains(v)) {
                        lowerLayout.layoutVariable(v);
                    }
                }

                tree.putAll(lowerLayout.tree);
                vertOffset = lowerLayout.getMaxY() + (ent.getValue().size() > 0 ? spacing : 0);
            }

            this.layoutTree = tree;
            resetPreferredSize();
            this.repaint();
            Collections.reverse(this.trace.frames);
        });


    }




    private Node renderNode(HeapEntity ent) {
        return renderNode(ent, 200);
    }

    private Node renderNode(HeapEntity ent, int clipLength) {
        // if we already have an object, return it!
        if (this.nodes.containsKey(ent.id)) {
            return this.nodes.get(ent.id);
        }
        Node ret = null;
        switch (ent.type) {
            case STACK:
                HeapCollection heapStack = (HeapCollection) ent;
                StackNode sn = new StackNode();
                if (heapStack.items.size() > 0 && heapStack.items.stream().anyMatch(val -> val.type == Value.Type.REFERENCE)) {
                    sn.pointers = heapStack.items.stream().map(val -> {
                        Node ref = null;
                        if (val.type == Value.Type.NULL) {
                            ref = new NullNode();
                            this.nodes.put(getUniqueNegKey(), ref);
                        } else if (val.type == Value.Type.REFERENCE) {
                            ref = renderNode(this.trace.heap.get(val.reference));
                        }

                        GraphEdge edge = new GraphEdge(sn, ref, "");
                        this.edges.add(edge);
                        return edge;
                    }).collect(Collectors.toList());
                    Collections.reverse(sn.pointers);
                } else {
                    sn.primData = heapStack.items.stream().map(Value::toString).collect(Collectors.toList());
                    Collections.reverse(sn.primData);
                }
                this.nodes.put(heapStack.id, sn);
                ret = sn;
                break;
            case SET:
                HeapCollection heapSet = (HeapCollection) ent;
                if (heapSet.items.size() > 0 && heapSet.items.stream().anyMatch(val -> val.type == Value.Type.REFERENCE)) {
                    ObjectSetNode osn = new ObjectSetNode();
                    Set<GraphEdge> pointers = heapSet.items.stream().map(val -> {
                        Node ref = null;
                        if (val.type == Value.Type.NULL) {
                            ref = new NullNode();
                            this.nodes.put(getUniqueNegKey(), ref);
                        } else if (val.type == Value.Type.REFERENCE){
                            ref = renderNode(this.trace.heap.get(val.reference));
                        }

                        GraphEdge graphEdge = new GraphEdge(osn, ref, "");
                        this.edges.add(graphEdge);
                        return graphEdge;
                    }).collect(Collectors.toSet());
                    this.nodes.put(heapSet.id, osn);
                    osn.setPointers(pointers);
                    ret = osn;
                } else {
                    PrimitiveSetNode psn = new PrimitiveSetNode();
                    psn.setData(heapSet.items.stream().map(Value::toString).collect(Collectors.toSet()));
                    this.nodes.put(heapSet.id, psn);
                    ret = psn;
                }
                break;
            case LIST:
                HeapCollection heapList = (HeapCollection)ent;
                // Reference list (checks for at least one reference)
                if (heapList.items.size() > 0 && heapList.items.stream().anyMatch(val -> val.type == Value.Type.REFERENCE)) {
                    ObjectArrayNode oan = new ObjectArrayNode(100, 100, heapList.items.size());
                    this.nodes.put(heapList.id, oan);
                    Map<GraphEdge, Integer> pointers = new HashMap<>();
                    for (int i = 0; i < heapList.items.size(); i++) {
                        Node ref;
                        if (heapList.items.get(i).type == Value.Type.REFERENCE) {
                            ref = renderNode(trace.heap.get(heapList.items.get(i).reference));
                            GraphEdge ge = new GraphEdge(oan, ref, "[" + i + "]", heapList.items.get(i).referenceType);
                            pointers.put(ge, i);
                            edges.add(ge);
                        } else {
                            oan.nullIndices.add(i);
                        }
                    }
                    oan.setPointers(pointers);
                    ret = oan;
                    break;
                } else {
                    String[] vals = new String[heapList.items.size()];
                    for (int i = 0; i < heapList.items.size(); i++) {
                        vals[i] = heapList.items.get(i).toString();
                    }
                    PrimitiveArrayNode pan = new PrimitiveArrayNode(100, 100, vals);
                    this.nodes.put(heapList.id, pan);
                    ret = pan;
                    break;
                }
            case MAP:
                HeapMap heapMap = (HeapMap)ent;
                // check for any references
                if (heapMap.pairs.size() > 0 && heapMap.pairs.stream().anyMatch(pair -> pair.val.type == Value.Type.REFERENCE || pair.key.type == Value.Type.REFERENCE)) {
                    ObjectMapNode omn = new ObjectMapNode(100, 100);
                    if (heapMap.pairs.stream().anyMatch(pair -> pair.key.type == Value.Type.REFERENCE)) {
                        if (heapMap.pairs.stream().anyMatch(pair -> pair.val.type == Value.Type.REFERENCE)) {
                            HashMap<GraphEdge, GraphEdge> vals = new HashMap<>();
                            for (HeapMap.Pair p : heapMap.pairs) {
                                GraphEdge valEdge = new GraphEdge(omn, null, "", p.val.referenceType);
                                if (p.val.type == Value.Type.REFERENCE) {
                                    valEdge.dest = renderNode(trace.heap.get(p.val.reference));
                                } else {
                                    NullNode to = new NullNode();
                                    valEdge.dest = to;
                                    this.nodes.put(getUniqueNegKey(), to);
                                }

                                GraphEdge keyEdge = new GraphEdge(omn, null, "", p.val.referenceType);
                                if (p.key.type == Value.Type.REFERENCE) {
                                    keyEdge.dest = renderNode(trace.heap.get(p.key.reference));
                                } else {
                                    NullNode to = new NullNode();
                                    keyEdge.dest = to;
                                    this.nodes.put(getUniqueNegKey(), to);
                                }
                                this.edges.add(keyEdge);
                                this.edges.add(valEdge);

                                vals.put(keyEdge, valEdge);
                            }
                            omn.setObjData(vals);
                            // ref-ref map
                        } else {
                            LinkedHashMap<GraphEdge, String> vals = new LinkedHashMap<>();
                            for (HeapMap.Pair p : heapMap.pairs) {
                                GraphEdge keyEdge = new GraphEdge(omn, null, "", p.val.referenceType);
                                if (p.key.type == Value.Type.REFERENCE) {
                                    keyEdge.dest = renderNode(trace.heap.get(p.key.reference));
                                } else {
                                    NullNode to = new NullNode();
                                    keyEdge.dest = to;
                                    this.nodes.put(getUniqueNegKey(), to);
                                }

                                this.edges.add(keyEdge);
                                vals.put(keyEdge, p.val.toString());
                            }
                            omn.setPrimValData(vals);
                        }
                    } else {
                        HashMap<String, GraphEdge> vals = new HashMap<>();
                        for (HeapMap.Pair p : heapMap.pairs) {
                            GraphEdge ge;
                            if (p.val.type == Value.Type.REFERENCE) {
                                ge = new GraphEdge(omn, renderNode(trace.heap.get(p.val.reference)), "[" + p.key + "]", p.val.referenceType);
                            } else {
                                NullNode to = new NullNode();
                                ge = new GraphEdge(omn, to, "[" + p.key + "]", p.val.referenceType);
                                this.nodes.put(getUniqueNegKey(), to);
                            }
                            this.edges.add(ge);
                            vals.put(p.key.toString(), ge);
                        }
                        omn.setPrimData(vals);
                    }

                    this.nodes.put(heapMap.id, omn);
                    ret = omn;
                    break;
                } else { // do a primitive map
                    PrimitiveMapNode pmn = new PrimitiveMapNode(100, 100);
                    HashMap<String, String> vals = new HashMap<>();
                    for (HeapMap.Pair p : heapMap.pairs) {
                        vals.put(p.key.toString(), p.val.toString());
                    }
                    pmn.setData(vals);
                    this.nodes.put(heapMap.id, pmn);
                    ret = pmn;
                    break;
                }
            case OBJECT:
                HeapObject obj = (HeapObject)ent;
                ConcurrentHashMap<String, String> fields = new ConcurrentHashMap<>();
                if (obj.mapFields.size () != 0) {
                    ret = new HorizontalClassMapNode(obj.label);
                    ((HorizontalClassMapNode)ret).fields = fields;
                    for (Map.Entry<String, Value> entry : obj.mapFields.entrySet()) {
                        if (entry.getValue().type == Value.Type.REFERENCE) {
                            int clipLen = 200;
                            if (obj.label.contains("HeapCharBuffer")) {
                                clipLen = 2048;
                            }
                            GraphEdge ref = new GraphEdge(ret, renderNode(this.trace.heap.get(entry.getValue().reference), clipLen), entry.getKey(), entry.getValue().referenceType);
                            ((HorizontalClassMapNode)ret).refs.put(entry.getKey(), ref);
                            this.edges.add(ref);
                        }
                    }
                } else {
                    ret = new ClassNode(100, 100, obj.label, fields);
                }
                this.nodes.put(obj.id, ret);
                for (String key : obj.fields.keySet()) {
                    GraphEdge edge = null;
                    if (obj.fields.get(key).type == Value.Type.REFERENCE) {
                        edge = new GraphEdge(ret, renderNode(this.trace.heap.get(obj.fields.get(key).reference)), key, obj.fields.get(key).referenceType);
                        this.edges.add(edge);
                    } else if (obj.fields.get(key).type == Value.Type.HOLE) {
                        StackFrame fr = this.frameMap.get(obj.fields.get(key).holeDest);
                        CodeNode cn = new CodeNode(0, 0, obj.fields.get(key).holeString);
                        this.nodes.put(getUniqueNegKey(), cn);
                        fr.targeted = true;
                        edge = new GraphEdge(ret, cn, key, obj.fields.get(key).referenceType);
                        cn.holeDest = fr;
                        this.edges.add(edge);
                    } else if (obj.fields.get(key).type == Value.Type.NULL) {
                        if (obj.fields.get(key).referenceType.equals("*INTERNAL*")) {
                            fields.put(key, "null");
                        } else {
                            NullNode nn = new NullNode();
                            edge = new GraphEdge(ret, nn, key, obj.fields.get(key).referenceType);
                            this.edges.add(edge);
                            this.nodes.put(getUniqueNegKey(), nn);
                        }
                    } else {
                        fields.put(key, obj.fields.get(key).toString());
                    }
                    if (ret instanceof HorizontalClassMapNode && edge != null) {
                        ((HorizontalClassMapNode)ret).pointers.add(edge);
                    } else if (edge != null) {
                        ((ClassNode)ret).addPointer(edge);
                    }
                }
                break;
            case PRIMITIVE:
                HeapPrimitive prim = (HeapPrimitive)ent;
                PrimitiveNode pn = new PrimitiveNode(100, 100, prim.value.toString());
                this.nodes.put(prim.id, pn);
                ret = pn;
                break;
            default:
                return null;
        }


        // apply applicable logical visualizations.
        for (LogicalVisualization viz : LogicalVisualization.getEnabledVisualizations()) {
            Node repl = viz.applyBuild(ret, this.nodes, this.edges);
            if (repl != null) {
                return repl;
            }
        }

        // we've already laid out this element. highlight changes.
        if (this.lastNodes.containsKey(ent.id)) {
            ret.highlightChanges(this.lastNodes.get(ent.id));
        }
        return ret;
    }

    private long getUniqueNegKey() {
        return getUniqueNegKey(this.nodes);
    }

    public static long getUniqueNegKey(Map<Long, Node> nodes) {
        Random r = new Random();
        int c = r.nextInt(2000);
        while (nodes.containsKey((long)c)) {
            c = r.nextInt(2000);
        }
        return c;
    }

    public void paint(Graphics g) {
        Rectangle oldView = parent.getVisibleRect();
        g.clearRect(0, 0, this.getWidth(), this.getHeight());
        Graphics2D g2D = (Graphics2D) g;
        g2D.scale(scale, scale);

        Set<VariableNode> varCopy = variables.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        varCopy.addAll(otherVariables);
        varCopy.forEach(variableNode -> {
            if (variableNode != null) {
                variableNode.draw(g2D);
            }
        });

        Set<Node> nodeCopy = new HashSet<>(nodes.values());
        Set<GraphEdge> edgesCopy = new HashSet<>(edges);

        for (Node gNode : nodeCopy) {
            // render nodes whose source edges should be in front
            if (gNode.getRenderBehavior() == Node.RenderBehavior.BEFORE_EDGES) {
                gNode.drawNode(g2D);
            }
        }

        for (StackFrame sf : variables.keySet()) {
            sf.draw(g2D, 1000); // TODO: fix width
        }

        for (GraphEdge edge : edgesCopy) {
            edge.draw(g2D);
        }

        for (Node gNode : nodeCopy) {
            // Render nodes whose source edges should be behind
            if (gNode.getRenderBehavior() == Node.RenderBehavior.AFTER_EDGES) {
                gNode.drawNode(g2D);
            }
        }


        if (curCursor != null) {
            setCursor(curCursor);
        }

        resetPreferredSize();
        g2D.dispose();
        init = true;
        parent.scrollRectToVisible(oldView);
    }

    private void resetPreferredSize() {
        double max_x, max_y;
        try {
            max_x = this.nodes.values().stream()
                    .mapToDouble(node -> node.getX() + node.getWidth()).max().getAsDouble() + 100;
            max_y = this.nodes.values().stream()
                    .mapToDouble(node -> node.getY() + node.getHeight()).max().getAsDouble() + 100;
        } catch (NoSuchElementException e) {
            max_x = 500;
            max_y = 500;
        }

        setPreferredSize(new Dimension((int) (max_x * scale), (int) (max_y * scale)));
        setSize(new Dimension((int) (max_x * scale), (int) (max_y * scale)));
    }

    private Node getNodeInCursor(double x, double y) {
        for (Node g : nodes.values()) {
            if (g.contains(x, y))
                return g;
        }

        for (VariableNode g : variables.values().stream().flatMap(Collection::stream).collect(Collectors.toSet())) {
            if (g != null && g.contains(x, y))
                return g;
        }
        return null;
    }

    // this is the one we call before layout is run. effectively does the same thing, but we don't have
    // layoutTree yet, so we need to follow the edges
    private Set<Node> findDownstreamNodes(Node node) {
        HashSet<Node> result = new HashSet<Node>();
        findDownstreamNodes(node, result);
        return result;
    }

    private void findDownstreamNodes(Node node, Set<Node> result) {
        result.add(node);
        if (node == null || node.getChildren() == null) return;

        for (GraphEdge e : node.getChildren()) {
            if (!result.contains(e.dest))
                findDownstreamNodes(e.dest, result);
        }
    }

    private Set<Node> getDownstreamNodes(Node head) {
        if (layoutTree != null) {
            if (layoutTree.containsKey(head)) {
                Set<Node> downstream = new HashSet<>(Arrays.<Node>asList(layoutTree.get(head).stream().map((edge) -> edge.dest).toArray(Node[]::new)));
                Set<Node> further = new HashSet<>();
                for (Node n : downstream) {
                    further.addAll(getDownstreamNodes(n));
                }
                downstream.addAll(further);
                return downstream;
            } else {
                return new HashSet<>();
            }
        } else {
            return findDownstreamNodes(head);
        }
    }

    class MyMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            Node n = getNodeInCursor(e.getX() / scale, e.getY() / scale);
            selected = getDownstreamNodes(getNodeInCursor(e.getX()/scale, e.getY()/scale));
            selected.add(n);
            GraphCanvas.this.repaint();
            x1 = e.getX() / scale;
            y1 = e.getY() / scale;
        }

        public void mouseReleased(MouseEvent e) {
            selected = new HashSet<>();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                Set<Node> downstream = getDownstreamNodes(getNodeInCursor(e.getX(), e.getY()));
                boolean hide = getNodeInCursor(e.getX(), e.getY()).getChildren().stream().map(edge -> edge.dest).noneMatch(Node::isHidden);
                for (Node node : downstream) {
                    node.setHidden(hide);
                }
            }
        }
    }

    class MyMouseMotionListener extends MouseMotionAdapter {
        public void mouseDragged(MouseEvent e) {
            if (selected != null) {
                double x2 = e.getX() / scale;
                double y2 = e.getY() / scale;
                for (Node n : selected) {
                    if (n != null)
                        n.setPos(n.getX() + x2 - x1, n.getY() + y2 - y1);
                }
                x1 = x2;
                y1 = y2;
            }
            resetPreferredSize();
            GraphCanvas.this.repaint();
        }

        public void mouseMoved(MouseEvent e) {
            if (getNodeInCursor( e.getX() / scale, e.getY() / scale) != null) {
                curCursor = Cursor
                        .getPredefinedCursor(Cursor.HAND_CURSOR);
            } else {
                curCursor = Cursor.getDefaultCursor();
            }
            GraphCanvas.this.repaint();
        }
    }
}
