package edu.caltech.cms.intelliviz.graph;

import javax.swing.*;
import java.util.HashMap;

/**
 * UI Sandbox Tester
 * @author Devin Hartzell
 */
public class GraphFrame extends JFrame {

    public GraphFrame() {
        super();
        setSize(800, 600);
        GraphCanvas canvas = new GraphCanvas();
        add(canvas);

        HashMap<String, String> fields = new HashMap<>();
        fields.put("size", "1");
        fields.put("anotherAttr", "fifty");
        String[] names = {"Class Name One", "Longer Class Name Two", "Short", "Class"};
        for (int i = 0; i < 3; i++) {
            canvas.nodes.put((long)i, new ClassNode(100 + 100 * i, 100, names[i], fields));
        }
        canvas.nodes.put(3L, new PrimitiveArrayNode(100, 300, new String[]{"\"Longer String\"", "2", "3"}));
        canvas.nodes.put(4L, new ObjectArrayNode(100, 400, 5));

        // Need to figure out a better way of doing this, probably.
        for (int i = 1; i <= 3; i++) {
            GraphEdge ge = new GraphEdge(canvas.nodes.get(4L), canvas.nodes.get((long)i), "[" + (i-1) + "]");
            canvas.edges.add(ge);
            ((ObjectArrayNode)canvas.nodes.get(4L)).pointers[i-1] = ge;
        }


        PrimitiveMapNode pmn = new PrimitiveMapNode(400, 200);
        pmn.data = fields;
        canvas.nodes.put(5L, pmn);

        ObjectMapNode omn = new ObjectMapNode(400, 300);
        HashMap<String, GraphEdge> omnEdges = new HashMap<>();
        omnEdges.put("thing1", new GraphEdge(omn, canvas.nodes.get(0L), "[thing1]"));
        omnEdges.put("thing2", new GraphEdge(omn, canvas.nodes.get(1L), "[thing2]"));
        omn.data = omnEdges;
        canvas.nodes.put(6L, omn);
        canvas.edges.addAll(omnEdges.values());
        setVisible(true);
    }

    public static void main(String[] args) {
        new GraphFrame();
    }
}
