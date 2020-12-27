package edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers;

import com.aegamesi.java_visualizer.backend.Tracer;
import com.aegamesi.java_visualizer.backend.TracerUtils;
import com.aegamesi.java_visualizer.model.ExecutionTrace;
import com.aegamesi.java_visualizer.model.HeapCollection;
import com.aegamesi.java_visualizer.model.HeapEntity;
import com.aegamesi.java_visualizer.model.HeapObject;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.LogicalVisualization;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StackVisualization extends LogicalVisualization {

    private static final long FACTOR = 487L;

    public StackVisualization(Map<String, Map<String, String>> classParams, Map<String, Map<String, String>> interfaceParams) {
        super(classParams, interfaceParams);
    }

    @Override
    protected String getDisplayName() {
        return "Stack";
    }

    @Override
    protected HeapEntity applyOnTrace(ObjectReference ref, Tracer tracer, Map<String, String> params) {
        HeapObject parent = convertParent(ref, tracer, params);

        HeapCollection data = new HeapCollection();
        data.type = HeapEntity.Type.STACK; // XXX: or SET
        Iterator<Value> i = TracerUtils.getIterator(tracer.thread, ref, params.get("iteratorMethod"));
        data.label = "INTERNAL STACK";
        while (i.hasNext()) {
            data.items.add(tracer.convertValue("", i.next()));
        }

        // link two things with a reference, while abusing the heck out of large numbers/longs...
        Long id = FACTOR * ref.uniqueID();
        data.id = id;
        tracer.model.heap.put(id, data);

        com.aegamesi.java_visualizer.model.Value refValue = new com.aegamesi.java_visualizer.model.Value();
        refValue.type = com.aegamesi.java_visualizer.model.Value.Type.REFERENCE;
        refValue.reference = id;
        refValue.referenceType = "*INTERNAL*";

        parent.fields.put("elementData", refValue);

        return parent;
    }

    @Override
    protected Node applyOnBuild(Node ref, Map<Long, Node> nodes, List<GraphEdge> edges, Map<String, String> params) {
        return null; // do nothing
    }
}
