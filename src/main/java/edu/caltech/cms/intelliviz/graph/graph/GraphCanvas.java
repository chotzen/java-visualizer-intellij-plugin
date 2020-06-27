package edu.caltech.cms.intelliviz.graph.graph;

import com.aegamesi.java_visualizer.model.*;
import com.aegamesi.java_visualizer.model.Frame;
import com.aegamesi.java_visualizer.plugin.JavaVisualizerManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ui.JBColor;
import jdk.nashorn.internal.ir.ObjectNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.HashMap;

public class GraphCanvas extends JPanel {

    private double scale = 1.0;

    private HashMap<Long, INode> nodes;
    private ArrayList<GraphEdge> edges;
    private INode selected;
    private Cursor curCursor;

    private ExecutionTrace trace;


    private double x1, y1;

    public GraphCanvas() {
        super();

        this.nodes = new HashMap<>();
        this.edges = new ArrayList<>();

        setBackground(JBColor.WHITE);
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
        removeAll();

        buildUI();

        revalidate();
        repaint();
    }

    private void buildUI() {
        for (Frame fr : trace.frames) {
            System.out.println("=== Frame: " + fr.name + " ===");
            for (String v : fr.locals.keySet()) {
                System.out.println(fr.locals.get(v).toString());
                if (fr.locals.get(v).type == Value.Type.REFERENCE) {
                    renderNode(trace.heap.get(fr.locals.get(v).reference));
                    trace.recursivelyPrint(0, trace.heap.get(fr.locals.get(v).reference));
                }
                System.out.println();
            }
            System.out.println();
        }
    }

    public INode renderNode(HeapEntity ent) {
        switch (ent.type) {
            case LIST:
            case SET:
                HeapList heapList = (HeapList)ent;
                // Reference list
                if (heapList.items.size() > 0 && heapList.items.get(0).type == Value.Type.REFERENCE) {
                    ObjectArrayNode oan = new ObjectArrayNode(100, 100, 3);
                    this.nodes.put(heapList.id, oan);
                    GraphEdge[] pointers = new GraphEdge[heapList.items.size()];
                    for (int i = 0; i < heapList.items.size(); i++) { // Assume that if the first item's a reference, all the items are.
                        INode ref = renderNode(trace.heap.get(heapList.items.get(i).reference));
                        GraphEdge ge = new GraphEdge(oan, ref, "[" + i + "]");
                        pointers[i] = ge;
                        edges.add(ge);
                    }
                    oan.pointers = pointers;
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
                // once again, assume that the first pair is indicative of the rest
                if (heapMap.pairs.size() > 0 && heapMap.pairs.get(0).val.type == Value.Type.REFERENCE) {
                    ObjectMapNode omn = new ObjectMapNode(100, 100);
                    HashMap<String, GraphEdge> vals = new HashMap<>();
                    for (HeapMap.Pair p : heapMap.pairs) {
                        GraphEdge ge = new GraphEdge(omn, renderNode(trace.heap.get(p.val.reference)), "[" + p.key + "]");
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
                        this.edges.add(new GraphEdge(cn, renderNode(this.trace.heap.get(obj.fields.get(key).reference)), key));
                    } else {
                        fields.put(key, obj.fields.get(key).toString());
                    }
                }
                this.nodes.put(obj.id, cn);
                return cn;
            case PRIMITIVE:
            default:
                System.out.println("WE FOUND A PRIMITIVE!!! IMPLEMENT IT!!!");
                return null;
        }
    }

    public void paint(Graphics g) {
        g.clearRect(0, 0, this.getWidth(), this.getHeight());
        Graphics2D g2D = (Graphics2D) g;
        for (INode gNode : nodes.values()) {
            gNode.draw(g2D);
        }
        for (GraphEdge edge : edges) {
            edge.draw(g2D);
        }
        if (curCursor != null) {
            setCursor(curCursor);
        }
        g2D.dispose();
    }

    private INode getNodeInCursor(double x, double y) {
        INode result = null;
        for (INode g : nodes.values()) {
            if (g.contains(x, y))
                result = g;
        }
        return result;
    }

    class MyMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            selected = getNodeInCursor(e.getX(), e.getY());
            GraphCanvas.this.repaint();
            x1 = e.getX();
            y1 = e.getY();
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
                double x2 = e.getX();
                double y2 = e.getY();
                selected.setPos(selected.getX() + x2 - x1, selected.getY() + y2 - y1);
                x1 = x2;
                y1 = y2;
            }
            GraphCanvas.this.repaint();
        }

        public void mouseMoved(MouseEvent e) {
            if (getNodeInCursor( e.getX(), e.getY()) != null) {
                curCursor = Cursor
                        .getPredefinedCursor(Cursor.HAND_CURSOR);
            } else {
                curCursor = Cursor.getDefaultCursor();
            }
            GraphCanvas.this.repaint();
        }
    }
}

/*
    HashMap<String, String> fields = new HashMap<>();
        fields.put("size", "1");
                fields.put("anotherAttr", "fifty");
                String[] names = {"Class Name One", "Longer Class Name Two", "Short", "Class"};
                for (int i = 0; i < 3; i++) {
        nodes.add(new ClassNode(100 + 100 * i, 100, names[i], fields));
        }
        nodes.add(new PrimitiveArrayNode(100, 300, new String[]{"\"Longer String\"", "2", "3"}));
        nodes.add(new ObjectArrayNode(100, 400, 5));

        // Need to figure out a better way of doing this, probably.
        for (int i = 1; i <= 3; i++) {
        GraphEdge ge = new GraphEdge(nodes.get(4), nodes.get(i), "[" + (i-1) + "]");
        edges.add(ge);
        ((ObjectArrayNode)nodes.get(4)).pointers[i-1] = ge;
        }


        PrimitiveMapNode pmn = new PrimitiveMapNode(400, 200);
        pmn.data = fields;
        nodes.add(pmn);

        ObjectMapNode omn = new ObjectMapNode(400, 300);
        HashMap<String, GraphEdge> omnEdges = new HashMap<>();
        omnEdges.put("thing1", new GraphEdge(omn, nodes.get(0), "[thing1]"));
        omnEdges.put("thing2", new GraphEdge(omn, nodes.get(1), "[thing2]"));
        omn.data = omnEdges;
        nodes.add(omn);
        edges.addAll(omnEdges.values());
*/