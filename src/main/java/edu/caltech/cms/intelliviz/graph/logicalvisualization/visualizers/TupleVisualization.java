package edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers;

import com.aegamesi.java_visualizer.backend.Tracer;
import com.aegamesi.java_visualizer.model.HeapEntity;
import com.aegamesi.java_visualizer.model.HeapPrimitive;
import com.aegamesi.java_visualizer.model.Value;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.LogicalVisualization;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TupleVisualization extends LogicalVisualization {

    public TupleVisualization(Map<String, Map<String, String>> classParams, Map<String, Map<String, String>> interfaceParams) {
        super(classParams, interfaceParams);
    }

    @Override
    protected String getDisplayName() {
        return "Tuple";
    }

    @Override
    protected HeapEntity applyOnTrace(ObjectReference ref, Tracer tracer, Map<String, String> params) {
        String data = "(";
        Iterator<Field> iter = ref.referenceType().allFields().iterator();
        while (iter.hasNext()) {
            Field f = iter.next();
            if (ref.getValue(f) instanceof ObjectReference) {
                return null;
            }
            data += tracer.convertValue("", ref.getValue(f)).toString();
            if (iter.hasNext()) {
                data += ", ";
            }
        }
        data += ")";

        HeapPrimitive out = new HeapPrimitive();
        out.type = HeapEntity.Type.PRIMITIVE;
        out.label = "String";
        out.value = new Value();
        out.value.type = Value.Type.TUPLE;
        out.value.stringValue = data;
        return out;
    }

    @Override
    protected Node applyOnBuild(Node ref, Map<Long, Node> nodes, List<GraphEdge> edges, Map<String, String> params) {
        return null;
    }
}
