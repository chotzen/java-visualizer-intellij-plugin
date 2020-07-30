package edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers;

import com.aegamesi.java_visualizer.backend.Tracer;
import com.aegamesi.java_visualizer.backend.TracerUtils;
import com.aegamesi.java_visualizer.model.*;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import edu.caltech.cms.intelliviz.graph.INode;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.GraphStruct;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.HeapStruct;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.LogicalVisualization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class SetVisualization extends LogicalVisualization {

    public SetVisualization(Map<String, Map<String, String>> classParams, Map<String, Map<String, String>> interfaceParams) {
        super(classParams, interfaceParams);
    }

    @Override
    protected String getDisplayName() {
        return null;
    }

    @Override
    protected HeapEntity applyOnTrace(ObjectReference ref, ThreadReference thread, ExecutionTrace trace, Map<String, String> params) {
        HeapObject parent = new HeapObject();
        parent.fields = new HashMap<>();
        com.sun.jdi.Value sizeValue = TracerUtils.invokeSimple(thread, ref, params.get("sizeMethod"));
        Value convSize = Tracer.convertValue(sizeValue);
        parent.fields.put("size", convSize);
        parent.interfaces = new HashSet<>();
        parent.id = ref.uniqueID();
        parent.type = HeapEntity.Type.OBJECT;

        HeapSet data = new HeapSet();
        data.type = HeapEntity.Type.SET; // XXX: or SET
        data.label = TracerUtils.displayNameForType(ref);
        Iterator<com.sun.jdi.Value> i = TracerUtils.getIterator(thread, ref, params.get("iterator"));
        while (i.hasNext()) {
            data.items.add(Tracer.convertValue(i.next()));
        }

        // link two things with a reference
        Long id = getUniqueNegKey(trace);
        trace.heap.put(id, data);

        Value refValue = new Value();
        refValue.type = Value.Type.REFERENCE;
        refValue.reference = id;
        refValue.referenceType = "*INTERNAL*";

        return parent;
    }

    @Override
    protected GraphStruct applyOnBuild(INode ref, Map<String, String> params) {
        // do nothing here
        return null;
    }
}
