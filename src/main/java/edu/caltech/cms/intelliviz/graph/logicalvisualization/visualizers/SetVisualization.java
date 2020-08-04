package edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers;

import com.aegamesi.java_visualizer.backend.Tracer;
import com.aegamesi.java_visualizer.backend.TracerUtils;
import com.aegamesi.java_visualizer.model.*;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.INode;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.GraphStruct;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.LogicalVisualization;

import java.util.*;

public class SetVisualization extends LogicalVisualization {

    public SetVisualization(Map<String, Map<String, String>> classParams, Map<String, Map<String, String>> interfaceParams) {
        super(classParams, interfaceParams);
    }

    @Override
    protected String getDisplayName() {
        return "Set";
    }

    @Override
    protected HeapEntity applyOnTrace(ObjectReference ref, ThreadReference thread, ExecutionTrace trace, Map<String, String> params) {
        HeapObject parent = convertParent(ref, thread, trace, params);

        HeapSet data = new HeapSet();
        data.type = HeapEntity.Type.SET; // XXX: or SET
        Iterator<com.sun.jdi.Value> i = TracerUtils.getIterator(thread, ref, params.get("iteratorMethod"));
        data.label = "INTERNAL SET";
        while (i.hasNext()) {
            data.items.add(Tracer.convertValue(i.next()));
        }

        // link two things with a reference
        Long id = getUniqueNegKey(trace);
        data.id = id;
        trace.heap.put(id, data);

        Value refValue = new Value();
        refValue.type = Value.Type.REFERENCE;
        refValue.reference = id;
        refValue.referenceType = "*INTERNAL*";

        parent.fields.put("elementData", refValue);

        return parent;
    }

    @Override
    protected INode applyOnBuild(INode ref, Map<Long, INode> nodes, List<GraphEdge> edges, Map<String, String> params) {
        return null;
    }
}
