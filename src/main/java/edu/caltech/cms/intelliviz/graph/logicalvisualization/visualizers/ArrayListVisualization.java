package edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers;


import com.aegamesi.java_visualizer.backend.Tracer;
import com.aegamesi.java_visualizer.model.ExecutionTrace;
import com.aegamesi.java_visualizer.model.HeapEntity;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.LogicalVisualization;
import edu.caltech.cms.intelliviz.graph.ui.ClassNode;
import edu.caltech.cms.intelliviz.graph.ui.NullNode;
import edu.caltech.cms.intelliviz.graph.ui.ObjectArrayNode;
import edu.caltech.cms.intelliviz.graph.ui.PrimitiveArrayNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArrayListVisualization extends LogicalVisualization {
    public ArrayListVisualization(Map<String, Map<String, String>> classParams, Map<String, Map<String, String>> interfaceParams) {
        super(classParams, interfaceParams);
    }

    @Override
    protected String getDisplayName() {
        return "ArrayList";
    }

    @Override
    protected HeapEntity applyOnTrace(ObjectReference ref, Tracer tracer, Map<String, String> params) {
        return null; // do nothing
    }

    @Override
    protected Node applyOnBuild(Node ref, Map<Long, Node> nodes, List<GraphEdge> edges, Map<String, String> params) {
        // java.util.ArrayList
        if (ref instanceof ClassNode) {
            // remove modCount from fields. that's all.
            ClassNode cRef = (ClassNode) ref;
            cRef.fields.remove("modCount");
            Node arrRef = cRef.pointers.get(0).dest;
            if (arrRef instanceof PrimitiveArrayNode) {
                PrimitiveArrayNode pan = (PrimitiveArrayNode) arrRef;
                String[] newValues = Arrays.stream(pan.values).filter(val -> !val.equals("null")).toArray(String[]::new);
                cRef.pointers.get(0).dest = new PrimitiveArrayNode((int)pan.getX(), (int)pan.getY(), newValues);
                nodes.put(getID(nodes, pan), cRef.pointers.get(0).dest);
            } else if (arrRef instanceof ObjectArrayNode){
                ObjectArrayNode oan = (ObjectArrayNode) arrRef;
                List<GraphEdge> newPointers = oan.pointers.stream().filter(edge -> {
                   if (!(edge.dest instanceof NullNode)) {
                       return true;
                   } else {
                       edges.remove(edge);
                       nodes.remove(getID(nodes, edge.dest));
                       return false;
                   }
                }).collect(Collectors.toList());
                cRef.pointers.get(0).dest = new ObjectArrayNode((int)oan.getX(), (int)oan.getY(), newPointers.size());
                ((ObjectArrayNode)cRef.pointers.get(0).dest).pointers = newPointers;
                newPointers.forEach(edge -> edge.source = cRef.pointers.get(0).dest);
                nodes.put(getID(nodes, oan), cRef.pointers.get(0).dest);
            }
            return ref;
        }
        return null;
    }

    private long getID(Map<Long, Node> nodes, Node node) {
        for (Map.Entry<Long, Node> entry : nodes.entrySet()) {
            if (entry.getValue().equals(node)) {
                return entry.getKey();
            }
        }
        return 0;
    }
}
