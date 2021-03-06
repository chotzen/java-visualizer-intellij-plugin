package edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers;

import com.aegamesi.java_visualizer.backend.Tracer;
import com.aegamesi.java_visualizer.backend.TracerUtils;
import com.aegamesi.java_visualizer.model.ExecutionTrace;
import com.aegamesi.java_visualizer.model.HeapEntity;
import com.aegamesi.java_visualizer.model.HeapMap;
import com.aegamesi.java_visualizer.model.HeapObject;
import com.sun.jdi.*;
import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.LogicalVisualization;

import java.util.*;

import static com.aegamesi.java_visualizer.backend.TracerUtils.invokeSimple;

public class MapVisualization extends LogicalVisualization {

    public MapVisualization(Map<String, Map<String, String>> classParams, Map<String, Map<String, String>> interfaceParams) {
        super(classParams, interfaceParams);
    }

    @Override
    protected String getDisplayName() {
        return "Map";
    }

    @Override
    protected HeapEntity applyOnTrace(ObjectReference ref, Tracer tracer, Map<String, String> params) {
        HeapObject parent = convertParent(ref, tracer, params);

        HeapMap child = new HeapMap();
        child.type = HeapEntity.Type.MAP;
        child.label = TracerUtils.displayNameForType(ref);

        ObjectReference keySet = (ObjectReference) invokeSimple(tracer.thread, ref, params.get("keySetMethod"));
        Iterator<Value> i = TracerUtils.getIterator(tracer.thread, keySet, params.get("keySetIteratorMethod"));

        while (i.hasNext()) {
            ObjectReference key = (ObjectReference) i.next();
            HeapMap.Pair pair = new HeapMap.Pair();
            pair.key = tracer.convertValue("", key);
            pair.val = tracer.convertValue("", invokeGet(tracer.thread, ref, params.get("getMethod"), key));
            pair.id = ((IntegerValue)TracerUtils.invokeSimple(tracer.thread, key, "hashCode")).value();
            child.pairs.add(pair);
        }

        child.pairs.sort(Comparator.comparing(c -> c.id));

        Long id = 479L * ref.uniqueID();
        child.id = id;
        tracer.model.heap.put(id, child);

        com.aegamesi.java_visualizer.model.Value refValue = new com.aegamesi.java_visualizer.model.Value();
        refValue.type = com.aegamesi.java_visualizer.model.Value.Type.REFERENCE;
        refValue.reference = id;
        refValue.referenceType = "*INTERNAL*";

        parent.fields.put("elementData", refValue);

        return parent;
    }

    @Override
    protected Node applyOnBuild(Node ref, Map<Long, Node> nodes, List<GraphEdge> edges, Map<String, String> params) {
        return null;
    }

    private com.sun.jdi.Value invokeGet(ThreadReference thread, ObjectReference ref, String methodName, com.sun.jdi.Value arg) {
        try {
            List<Value> newList = new ArrayList<>();
            newList.add(arg);
            return ref.invokeMethod(thread, ref.referenceType().methodsByName(methodName).get(0), newList, 0);
        } catch (Exception e) {
            return null;
        }
    }
}
