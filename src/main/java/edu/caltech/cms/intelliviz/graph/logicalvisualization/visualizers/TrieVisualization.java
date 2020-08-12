package edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers;

import com.aegamesi.java_visualizer.backend.Tracer;
import com.aegamesi.java_visualizer.backend.TracerUtils;
import com.aegamesi.java_visualizer.model.ExecutionTrace;
import com.aegamesi.java_visualizer.model.HeapEntity;
import com.aegamesi.java_visualizer.model.HeapObject;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.LogicalVisualization;
import edu.caltech.cms.intelliviz.graph.ui.ObjectArrayNode;
import kotlin.jvm.internal.Ref;

import java.util.*;

public class TrieVisualization extends LogicalVisualization {

    public TrieVisualization(Map<String, Map<String, String>> classParams, Map<String, Map<String, String>> interfaceParams) {
        super(classParams, interfaceParams);
    }

    @Override
    protected String getDisplayName() {
        return "Trie";
    }

    @Override
    protected HeapEntity applyOnTrace(ObjectReference ref, Tracer tracer, Map<String, String> params) {
        // PLAN
        // Read references from interior map
        // Do a recursive call to convertObject on them because that's how this works (need to be able to see convertObject from here, though)
        // SCHEMATIC
        // TrieNode -- pointers --> HashMap --> entrySet --> iterator --> convertObject on references

        HeapObject nodeObj = new HeapObject();
        nodeObj.type = HeapEntity.Type.OBJECT;
        nodeObj.id = ref.uniqueID();
        nodeObj.label = "edu.caltech.cs2.datastructures.TrieNode";

        Map<Field, com.sun.jdi.Value> fields = ref.getValues(ref.referenceType().allFields());
        for (Map.Entry<Field, Value> entry : fields.entrySet()) {
            if (entry.getKey().name().equals("value")) {
                if (entry.getValue() == null) {
                    com.aegamesi.java_visualizer.model.Value nullVal = new com.aegamesi.java_visualizer.model.Value();
                    nullVal.type = com.aegamesi.java_visualizer.model.Value.Type.NULL;
                    nullVal.referenceType = "*INTERNAL*";
                    nodeObj.fields.put("value", nullVal);
                } else {
                    nodeObj.fields.put("value", tracer.convertValue(entry.getValue()));
                }
            }
        }

        Optional<Value> pointersOpt = fields.entrySet().stream().filter(entr -> entr.getKey().name().equals("pointers"))
                                    .map(Map.Entry::getValue)
                                    .findFirst();

        assert pointersOpt.get() instanceof ObjectReference;

        ObjectReference pointersMap = (ObjectReference) pointersOpt.get();
        Value entrySet = TracerUtils.invokeSimple(tracer.thread, pointersMap, "entrySet");

        assert entrySet instanceof ObjectReference;
        ObjectReference valueSetObj = (ObjectReference) entrySet;
        Iterator<Value> iter = TracerUtils.getIterator(tracer.thread, valueSetObj, "iterator");

        while (iter.hasNext()) {
            ObjectReference entr = (ObjectReference)iter.next();
            Value keyVal = TracerUtils.invokeSimple(tracer.thread, entr, "getKey");
            String key = tracer.convertValue(keyVal).toString();

            Value valVal = TracerUtils.invokeSimple(tracer.thread, entr, "getValue");
            com.aegamesi.java_visualizer.model.Value newRef =  tracer.convertValue(valVal); // next TrieNode

            nodeObj.mapFields.put(key, newRef);
        }

        return nodeObj;
    }

    @Override
    protected Node applyOnBuild(Node ref, Map<Long, Node> nodes, List<GraphEdge> edges, Map<String, String> params) {
        // TODO embiggen edges
        return null;
    }
}
