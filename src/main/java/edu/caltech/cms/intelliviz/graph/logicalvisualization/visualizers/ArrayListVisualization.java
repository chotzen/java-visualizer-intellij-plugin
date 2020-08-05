package edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers;


import com.aegamesi.java_visualizer.model.ExecutionTrace;
import com.aegamesi.java_visualizer.model.HeapEntity;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.INode;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.LogicalVisualization;
import edu.caltech.cms.intelliviz.graph.ui.ClassNode;

import java.util.List;
import java.util.Map;

public class ArrayListVisualization extends LogicalVisualization {
    public ArrayListVisualization(Map<String, Map<String, String>> classParams, Map<String, Map<String, String>> interfaceParams) {
        super(classParams, interfaceParams);
    }

    @Override
    protected String getDisplayName() {
        return "ArrayList";
    }

    @Override
    protected HeapEntity applyOnTrace(ObjectReference ref, ThreadReference thread, ExecutionTrace trace, Map<String, String> params) {
        return null; // do nothing
    }

    @Override
    protected INode applyOnBuild(INode ref, Map<Long, INode> nodes, List<GraphEdge> edges, Map<String, String> params) {
        // java.util.ArrayList
        if (ref instanceof ClassNode) {
            // remove modCount from fields. that's all.
            ((ClassNode)ref).fields.remove("modCount");
            return ref;
        }
        return null;
    }
}
