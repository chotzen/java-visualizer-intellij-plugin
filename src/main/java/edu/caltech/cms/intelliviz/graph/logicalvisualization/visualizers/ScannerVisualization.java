package edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers;

import com.aegamesi.java_visualizer.backend.Tracer;
import com.aegamesi.java_visualizer.model.*;
import com.aegamesi.java_visualizer.model.Value;
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter;
import com.intellij.util.io.CharSequenceBackedByChars;
import com.sun.jdi.*;
import edu.caltech.cms.intelliviz.graph.GraphCanvas;
import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.LogicalVisualization;
import edu.caltech.cms.intelliviz.graph.ui.ClassNode;
import edu.caltech.cms.intelliviz.graph.ui.PrimitiveArrayNode;
import edu.caltech.cms.intelliviz.graph.ui.ScannerNode;

import java.util.*;
import java.util.stream.Collectors;

public class ScannerVisualization extends LogicalVisualization {

    public ScannerVisualization(Map<String, Map<String, String>> classParams, Map<String, Map<String, String>> interfaceParams) {
        super(classParams, interfaceParams);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner("abcdefghijklmnopqrstuvwxyz");
        while (sc.hasNext()) {
            System.out.println(sc.next());
        }
    }

    @Override
    protected String getDisplayName() {
        return "Scanner";
    }

    @Override
    protected HeapEntity applyOnTrace(ObjectReference ref, Tracer tracer, Map<String, String> params) {
        /*
        plan for doing this
        - scanner class
        - has field called `buf`
        - has char array called `hb`
        - read it
         */

        HeapObject obj = new HeapObject();
        obj.label = "java.util.Scanner";
        obj.id = ref.uniqueID();
        obj.type = HeapEntity.Type.OBJECT;
        // calculate position
        Field position = ref.referenceType().fieldByName("position");
        obj.fields.put("position", tracer.convertValue(ref.getValue(position)));

        // follow other paths or something
        Field bufField = ref.referenceType().fieldByName("buf");
        ObjectReference bufRef = (ObjectReference)ref.getValue(bufField);
        Field hbField = bufRef.referenceType().fieldByName("hb");
        ArrayReference ar = (ArrayReference)bufRef.getValue(hbField);
        String blob = ar.getValues().stream().map(v -> ((CharValue)v).value())
                .filter(c -> (32 <= c && c <= 126) || c == 10 || c == 13)
                .map(String::valueOf)
                .collect(Collectors.joining());

        Value out = new Value();
        out.type = Value.Type.SCANNER_BLOB;
        out.stringValue = blob;
        out.scannerPos = Integer.valueOf(tracer.convertValue(ref.getValue(position)).toString());

        obj.fields.put("data", out);

        return obj;
    }

    @Override
    protected Node applyOnBuild(Node ref, Map<Long, Node> nodes, List<GraphEdge> edges, Map<String, String> params) {
        /*
        // no need for params, since this only acts on java.util.Scanner
        if (ref instanceof ClassNode) {
            ClassNode scanner = (ClassNode) ref;
            int position = Integer.parseInt(scanner.fields.get("position"));
            // scanner -> buf (type HeapCharBuffer or something) -> children[0], which is a primitivearraynode
            ClassNode bufNode = (ClassNode) scanner.getChildren().stream().filter(edge -> edge.label.toString().equals("buf"))
                    .map(edge -> edge.dest).findFirst().get();
            PrimitiveArrayNode charArray = (PrimitiveArrayNode)bufNode.getChildren().get(0).dest;
            StringBuilder sb = new StringBuilder();
            for (String str : charArray.values) {
                char c = str.charAt(1);
                if ((32 <= c && c <= 126) || c == 10 || c == 13) {
                    sb.append(c);
                }
            }
            String content = sb.toString();

            // cleanse the canvas of our sins
            scanner.pointers.forEach(edge -> {
                prune(edge.dest, nodes, edges);
                edges.remove(edge);
            });

            scanner.fields.clear();
            scanner.fields.put("position", String.valueOf(position));

            ScannerNode scNode = new ScannerNode(content, position);
            GraphEdge edge = new GraphEdge(scanner, scNode, "contents", "INTERNAL");
            scanner.pointers.clear();
            scanner.pointers.add(edge);

            edges.add(edge);
            nodes.put(GraphCanvas.getUniqueNegKey(nodes), scNode);

            return scanner;
        }

         */
        return null;
    }

    private void prune(Node root, Map<Long, Node> nodes, List<GraphEdge> edges) {
        // remove this node from the map
        Set<Long> toRemove = nodes.entrySet().stream()
                .filter(ent -> ent.getValue().equals(root))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        toRemove.forEach(nodes::remove);

        // do the same thing to children
        root.getChildren().forEach(edge -> {
            if (nodes.containsValue(edge.dest)) {
                prune(edge.dest, nodes, edges);
            }
        });

        // remove this node's edges from the map
        root.getChildren().forEach(edges::remove);
    }
}
