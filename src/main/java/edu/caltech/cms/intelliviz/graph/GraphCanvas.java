package edu.caltech.cms.intelliviz.graph;

import com.aegamesi.java_visualizer.model.*;
import com.aegamesi.java_visualizer.model.Frame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class GraphCanvas extends JPanel {

    private double scale = 1.0;

    private Map<Frame, StackFrame> frameMap;
    private LinkedHashMap<StackFrame, List<VariableNode>> variables;
    private Map<Long, INode> nodes;
    private List<GraphEdge> edges;
    private Map<INode, List<GraphEdge>> layoutTree;

    private Set<INode> selected;
    private Cursor curCursor;

    private ExecutionTrace trace;

    private Graphics grRef;

    private double x1, y1;
    private boolean init = false;

    public GraphCanvas() {
        super();
        this.grRef = getGraphics();

        this.variables = new LinkedHashMap<>();
        this.nodes = new HashMap<>();
        this.edges = new ArrayList<>();
        this.frameMap = new HashMap<>();

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

    private void refreshUI() {
        nodes.clear(); // TODO: preserve nodes that didn't get deleted, probably by their ID
        edges.clear();
        variables.clear();


        removeAll();

        buildUI();

        revalidate();
        repaint();
    }

    private void buildUI() {

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
                if (fr.locals.get(v).type == Value.Type.REFERENCE) {
                    if (!v.equals("this") || thisNode == null) {
                        VariableNode var = new VariableNode(0, 0, v, renderNode(trace.heap.get(fr.locals.get(v).reference)), fr.locals.get(v).referenceType);
                        System.out.println(var.name + ": " + var.declaringType);
                        if (v.equals("this")) {
                            thisNode = var;
                        }
                        if (frameMap.size() != this.variables.size()) {
                            System.out.println("breakpoint here this is weird");
                        }
                        this.variables.get(convert).add(var);
                    }
                } else if (fr.locals.get(v).type == Value.Type.HOLE) {
                    StackFrame dest = this.frameMap.get(fr.locals.get(v).holeDest);
                    VariableNode var = new VariableNode(0, 0, v, dest, fr.locals.get(v).type.toString());
                    dest.targeted = true;
                    this.variables.get(convert).add(var);
                } else {
                    PrimitiveNode node = new PrimitiveNode(0, 0, fr.locals.get(v).toString());
                    VariableNode var = new VariableNode(0, 0, v, node, fr.locals.get(v).type.toString());
                    this.variables.get(convert).add(var);
                    this.nodes.put(getUniqueNegKey(), node);
                }
            }
        }
        init = false;

        Set<INode> allDownstream = new HashSet<>();
        Map<StackFrame, Set<INode>> downstreams = new HashMap<>();

        List<Map.Entry<StackFrame, List<VariableNode>>> list = new ArrayList<>(this.variables.entrySet());
        Collections.reverse(list);

        for (Map.Entry<StackFrame, List<VariableNode>> ent : list) {
            // Map each stackframe to downstream nodes that have not yet been searched.
            // this works because LinkedHashMap iterates based on insertion order, and
            // this.variables is a LinkedHashMap, probably.
            VariableNode finalThisNode1 = thisNode;
            downstreams.put(ent.getKey(), new HashSet<>(
                    ent.getValue().stream()
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

            HashMap<INode, List<GraphEdge>> tree = new HashMap<>();
            double vertOffset = spacing;
            double horizOffset = 0;
            if (finalThisNode != null) {
                GraphLayoutAlgorithm upperLayout = new GraphLayoutAlgorithm(20, 20, allDownstream);
                upperLayout.layoutVariable(finalThisNode);
                tree.putAll(upperLayout.tree);
                vertOffset = upperLayout.getMaxY() + spacing;
            }

            for (Map.Entry<StackFrame, List<VariableNode>> ent : this.variables.entrySet()) {
                allDownstream.removeAll(downstreams.get(ent.getKey()));

                ent.getKey().vertOffset = (int) vertOffset;
                System.out.println(vertOffset);
                Point2D origin = ent.getKey().getOrigin();
                GraphLayoutAlgorithm lowerLayout = new GraphLayoutAlgorithm(origin.getX(), origin.getY(), allDownstream);

                for (VariableNode v: ent.getValue()) {
                    if (!v.equals(finalThisNode)) {
                        lowerLayout.layoutVariable(v);
                    }
                }

                tree.putAll(lowerLayout.tree);
                vertOffset = lowerLayout.getMaxY() + ent.getValue().size() > 0 ? spacing : 0;

            }

            this.layoutTree = tree;
            resetPreferredSize();
            this.repaint();
        });

    }

    INode renderNode(HeapEntity ent) {
        // if we already have an object, return it!
        System.out.println(ent.id);
        if (this.nodes.containsKey(ent.id)) {
            return this.nodes.get(ent.id);
        }
        switch (ent.type) {
            case LIST:
            case SET:
                HeapList heapList = (HeapList)ent;
                // Reference list (checks for at least one reference)
                if (heapList.items.size() > 0 && heapList.items.stream().anyMatch(val -> val.type == Value.Type.REFERENCE)) {
                    ObjectArrayNode oan = new ObjectArrayNode(100, 100, heapList.items.size());
                    this.nodes.put(heapList.id, oan);
                    for (int i = 0; i < heapList.items.size(); i++) {
                        INode ref;
                        if (heapList.items.get(i).type == Value.Type.REFERENCE) {
                            ref = renderNode(trace.heap.get(heapList.items.get(i).reference));
                        } else {
                            ref = new NullNode();
                            this.nodes.put(getUniqueNegKey(), ref);
                        }
                        GraphEdge ge = new GraphEdge(oan, ref, "[" + i + "]", heapList.items.get(i).referenceType);
                        oan.addPointer(ge);
                        edges.add(ge);
                    }
                    return oan;
                } else {
                    String[] vals = new String[heapList.items.size()];
                    for (int i = 0; i < heapList.items.size(); i++) {
                        vals[i] = heapList.items.get(i).toString();
                    }
                    PrimitiveArrayNode pan = new PrimitiveArrayNode(100, 100, vals);
                    this.nodes.put(heapList.id, pan);
                    return pan;
                }
            case MAP:
                HeapMap heapMap = (HeapMap)ent;
                // check for any references
                if (heapMap.pairs.size() > 0 && heapMap.pairs.stream().anyMatch(pair -> pair.val.type == Value.Type.REFERENCE)) {
                    ObjectMapNode omn = new ObjectMapNode(100, 100);
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
                    omn.data = vals;
                    this.nodes.put(heapMap.id, omn);
                    return omn;
                } else { // do a primitive map
                    PrimitiveMapNode pmn = new PrimitiveMapNode(100, 100);
                    HashMap<String, String> vals = new HashMap<>();
                    for (HeapMap.Pair p : heapMap.pairs) {
                        vals.put(p.key.toString(), p.val.toString());
                    }
                    pmn.data = vals;
                    this.nodes.put(heapMap.id, pmn);
                    return pmn;
                }
            case OBJECT:
                HeapObject obj = (HeapObject)ent;
                HashMap<String, String> fields = new HashMap<>();
                ClassNode cn = new ClassNode(100, 100, obj.label, fields);
                this.nodes.put(obj.id, cn);
                for (String key : obj.fields.keySet()) {
                    if (obj.fields.get(key).type == Value.Type.REFERENCE) {
                        GraphEdge edge = new GraphEdge(cn, renderNode(this.trace.heap.get(obj.fields.get(key).reference)), key, obj.fields.get(key).referenceType);
                        cn.addPointer(edge);
                        this.edges.add(edge);
                    } else if (obj.fields.get(key).type == Value.Type.HOLE) {
                        StackFrame fr = this.frameMap.get(obj.fields.get(key).holeDest);
                        fr.targeted = true;
                        GraphEdge edge = new GraphEdge(cn, fr, key, obj.fields.get(key).referenceType);
                        cn.addPointer(edge);
                        this.edges.add(edge);
                    } else if (obj.fields.get(key).type == Value.Type.NULL) {
                        NullNode nn = new NullNode();
                        GraphEdge edge = new GraphEdge(cn, nn, key, obj.fields.get(key).referenceType);
                        cn.addPointer(edge);
                        this.edges.add(edge);
                        this.nodes.put(getUniqueNegKey(), nn);
                    } else {
                        fields.put(key, obj.fields.get(key).toString());
                    }
                }
                cn.init();
                return cn;
            case PRIMITIVE:
                HeapPrimitive prim = (HeapPrimitive)ent;
                PrimitiveNode pn = new PrimitiveNode(100, 100, prim.value.toString());
                this.nodes.put(prim.id, pn);
                return pn;
            default:
                return null;
        }
    }

    private long getUniqueNegKey() {
        Random r = new Random();
        int c = r.nextInt(2000);
        while (nodes.containsKey((long)c)) {
            c = r.nextInt(2000);
        }
        return c;
    }

    public void paint(Graphics g) {
        g.clearRect(0, 0, this.getWidth(), this.getHeight());
        Graphics2D g2D = (Graphics2D) g;
        g2D.scale(scale, scale);

        variables.values().stream().flatMap(Collection::stream).forEach(vNode -> vNode.draw(g2D));

        for (INode gNode : nodes.values()) {
            // render nodes whose source edges should be in front
            if (gNode instanceof ObjectMapNode || gNode instanceof ObjectArrayNode) {
                gNode.draw(g2D);
            }
        }

        for (GraphEdge edge : edges) {
            edge.draw(g2D);
        }

        for (INode gNode : nodes.values()) {
            // Render nodes whose source edges should be behind
            if (!(gNode instanceof ObjectMapNode || gNode instanceof ObjectArrayNode)) {
                gNode.draw(g2D);
            }
        }

        for (StackFrame sf : variables.keySet()) {
            sf.draw(g2D, 1000); // TODO: fix width
        }

        if (curCursor != null) {
            setCursor(curCursor);
        }

        resetPreferredSize();
        g2D.dispose();
        init = true;
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

    private INode getNodeInCursor(double x, double y) {
        for (INode g : nodes.values()) {
            if (g.contains(x, y))
                return g;
        }

        for (VariableNode g : variables.values().stream().flatMap(Collection::stream).collect(Collectors.toSet())) {
            if (g.contains(x, y))
                return g;
        }
        return null;
    }

    // this is the one we call before layout is run. effectively does the same thing, but we don't have
    // layoutTree yet, so we need to follow the edges
    Set<INode> findDownstreamNodes(INode node) {
        HashSet<INode> result = new HashSet<INode>();
        findDownstreamNodes(node, result);
        return result;
    }

    private void findDownstreamNodes(INode node, Set<INode> result) {
        result.add(node);
        if (node == null || node.getChildren() == null) return;

        for (GraphEdge e : node.getChildren()) {
            if (!result.contains(e.dest))
                findDownstreamNodes(e.dest, result);
        }
    }

    private Set<INode> getDownstreamNodes(INode head) {
        if (layoutTree != null) {
            if (layoutTree.containsKey(head)) {
                Set<INode> downstream = new HashSet<>(Arrays.<INode>asList(layoutTree.get(head).stream().map((edge) -> edge.dest).toArray(INode[]::new)));
                Set<INode> further = new HashSet<>();
                for (INode n : downstream) {
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
            INode n = getNodeInCursor(e.getX() / scale, e.getY() / scale);
            selected = getDownstreamNodes(getNodeInCursor(e.getX()/scale, e.getY()/scale));
            selected.add(n);
            GraphCanvas.this.repaint();
            x1 = e.getX() / scale;
            y1 = e.getY() / scale;
        }

        public void mouseReleased(MouseEvent e) {
            selected = new HashSet<>();
        }

    }

    class MyMouseMotionListener extends MouseMotionAdapter {
        public void mouseDragged(MouseEvent e) {
            if (selected != null) {
                double x2 = e.getX() / scale;
                double y2 = e.getY() / scale;
                for (INode n : selected) {
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
