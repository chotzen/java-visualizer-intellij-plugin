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

public class GraphCanvas extends JPanel {

    private double scale = 1.0;

    private ArrayList<VariableNode> variables;
    private HashMap<Long, INode> nodes;
    private List<GraphEdge> edges;
    private ArrayList<StackFrame> stackFrames;
    private HashMap<INode, ArrayList<GraphEdge>> layoutTree;

    private Set<INode> selected;
    private Cursor curCursor;

    private ExecutionTrace trace;

    private Graphics grRef;
    private double vertOffset = 0;

    private double x1, y1;
    private boolean init = false;

    public GraphCanvas() {
        super();
        this.grRef = getGraphics();

        this.variables = new ArrayList<>();
        this.nodes = new HashMap<>();
        this.edges = new ArrayList<>();
        this.stackFrames = new ArrayList<>();

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
        stackFrames.clear();

        removeAll();

        buildUI();

        revalidate();
        repaint();
    }

    private void buildUI() {

        // Initialize stack frames
        // Vertical offset will be determined by layout and passed to draw() from this.vertOffset
        for (int i = this.trace.frames.size() - 1; i >= 0; i--) {
            this.stackFrames.add(new StackFrame(this.trace.heap, this.trace.frames.get(i),
                    this.trace.frames.size() - i, i != this.trace.frames.size() - 1));

        }

        int primId = -1;

        Frame fr = this.trace.frames.get(0);
        for (String v : fr.locals.keySet()) {
            System.out.println(fr.locals.get(v).type);
            if (fr.locals.get(v).type == Value.Type.REFERENCE) {
                VariableNode var = new VariableNode(0, 0, v, renderNode(trace.heap.get(fr.locals.get(v).reference)));
                this.variables.add(var);
            } else {
                PrimitiveNode node = new PrimitiveNode(0, 0, fr.locals.get(v).toString());
                VariableNode var = new VariableNode(0, 0, v, node);
                this.variables.add(var);
                System.out.println(v);
                this.nodes.put((long) primId, node);
                primId--;
            }
        }
        init = false;

        this.paintImmediately(0, 0, 1000, 1000);

        while (!init) {
            int i = 1;
        }

        // get downstream nodes
        // layout "this" without nodes
        // get the height and reset stackframes
        // render other nodes below it

        Set<INode> downstreamNodes = new HashSet<>();
        VariableNode thisNode = null;
        for (VariableNode v : this.variables) {
            if (!v.name.equals("this")) {
                downstreamNodes.addAll(findDownstreamNodes(v.reference));
                downstreamNodes.add(v.reference);
            } else {
                thisNode = v;
            }
        }

        HashMap<INode, ArrayList<GraphEdge>> tree = new HashMap<>();
        if (thisNode != null) {
            GraphVisualizationAlgorithm upperLayout = new GraphVisualizationAlgorithm(20, 20, downstreamNodes);
            upperLayout.layoutVariable(thisNode);
            tree.putAll(upperLayout.tree);
            this.vertOffset = upperLayout.getMax_y();
        }

        Point2D origin = this.stackFrames.get(this.stackFrames.size() - 1).getOrigin((int)this.vertOffset);
        GraphVisualizationAlgorithm lowerLayout = new GraphVisualizationAlgorithm(origin.getX(), origin.getY(), new HashSet<>());

        for (VariableNode v: this.variables) {
            if (!v.equals(thisNode)) {
                lowerLayout.layoutVariable(v);
            }
        }

        double height = lowerLayout.getMax_y();
        setPreferredSize(new Dimension((int) (500 * scale), (int) (height * scale)));
        tree.putAll(lowerLayout.tree);
        this.layoutTree = tree;
    }

    public INode renderNode(HeapEntity ent) {
        // if we already have an object, return it!
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
                        GraphEdge ge = new GraphEdge(oan, ref, "[" + i + "]");
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
                            ge = new GraphEdge(omn, renderNode(trace.heap.get(p.val.reference)), "[" + p.key + "]");
                        } else {
                            NullNode to = new NullNode();
                            ge = new GraphEdge(omn, to, "[" + p.key + "]");
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
                for (String key : obj.fields.keySet()) {
                    if (obj.fields.get(key).type == Value.Type.REFERENCE) {
                        GraphEdge edge = new GraphEdge(cn, renderNode(this.trace.heap.get(obj.fields.get(key).reference)), key);
                        cn.addPointer(edge);
                        this.edges.add(edge);
                    } else if (obj.fields.get(key).type == Value.Type.NULL) {
                        NullNode nn = new NullNode();
                        GraphEdge edge = new GraphEdge(cn, nn, key);
                        cn.addPointer(edge);
                        this.edges.add(edge);
                        this.nodes.put(getUniqueNegKey(), nn);

                    } else {
                        fields.put(key, obj.fields.get(key).toString());
                    }
                }
                this.nodes.put(obj.id, cn);
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
        double max_x = 0, max_y = 0;
        for (INode gNode : nodes.values()) {
            // render nodes whose source edges should be in front
            if (gNode instanceof ObjectMapNode || gNode instanceof ObjectArrayNode) {
                gNode.draw(g2D);
            }

            max_x = Math.max(max_x, gNode.getX() + gNode.getWidth());
            max_y = Math.max(max_y, gNode.getY() + gNode.getHeight());
        }
        for (GraphEdge edge : edges) {
            edge.draw(g2D);
        }
        for (INode gNode : nodes.values()) {
            // Render nodes whose source edges should be behind
            if (!(gNode instanceof ObjectMapNode || gNode instanceof ObjectArrayNode)) {
                gNode.draw(g2D);
            }
            max_x = Math.max(max_x, gNode.getX() + gNode.getWidth());
            max_y = Math.max(max_y, gNode.getY() + gNode.getHeight());
        }
        for (StackFrame sf : stackFrames) {
            sf.draw(g2D, (int)this.vertOffset, 500);
        }
        for (int i = 0; i < variables.size(); i++) {
            variables.get(i).draw(g2D);
        }
        if (curCursor != null) {
            setCursor(curCursor);
        }
        max_x += 100;
        max_y += 100;
        setPreferredSize(new Dimension((int) (max_x * scale), (int) (max_y * scale)));
        g2D.dispose();
        init = true;
    }

    private INode getNodeInCursor(double x, double y) {
        for (INode g : nodes.values()) {
            if (g.contains(x, y))
                return g;
        }

        for (VariableNode g : variables) {
            if (g.contains(x, y))
                return g;
        }
        return null;
    }

    // this is the one we call before layout is run. effectively does the same thing, but we don't have
    // layoutTree yet, so we need to follow the edges
    private Set<INode> findDownstreamNodes(INode node) {
        HashSet<INode> result = new HashSet<INode>();
        findDownstreamNodes(node, result);
        return result;
    }


    private void findDownstreamNodes(INode node, Set<INode> result) {
        result.add(node);
        for (GraphEdge e : node.getChildren()) {
            if (!result.contains(e.dest))
                findDownstreamNodes(e.dest, result);
        }
    }


    private Set<INode> getDownstreamNodes(INode head) {
        if (layoutTree.get(head) != null) {
            Set<INode> downstream = new HashSet<>(Arrays.<INode>asList(layoutTree.get(head).stream().map((edge) -> edge.dest).toArray(INode[]::new)));
            Set<INode> further = new HashSet<>();
            for (INode n : downstream) {
                further.addAll(getDownstreamNodes(n));
            }
            downstream.addAll(further);
            return downstream;
        }
        return new HashSet<>();
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
            selected = null;
        }

        /*public void mouseClicked(MouseEvent e) {
            if (ellipse.contains(e.getX(), e.getY())) {
                selectedShape = ellipse;
                boundingRec = ellipse.getBounds2D();

            } else {
                if (boundingRec != null)
                    boundingRec = null;
            }
            GraphCanvas.this.repaint();
        } */
    }

    class MyMouseMotionListener extends MouseMotionAdapter {
        public void mouseDragged(MouseEvent e) {
            if (selected != null) {
                double x2 = e.getX() / scale;
                double y2 = e.getY() / scale;
                for (INode n : selected) {
                    n.setPos(n.getX() + x2 - x1, n.getY() + y2 - y1);
                }
                x1 = x2;
                y1 = y2;
            }
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
