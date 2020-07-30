package edu.caltech.cms.intelliviz.graph.logicalvisualization;

import com.aegamesi.java_visualizer.model.HeapEntity;
import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.INode;

import java.util.List;
import java.util.Map;

public class HeapStruct {
    public HeapEntity root;
    public List<HeapEntity> nodes;

    public HeapStruct(HeapEntity root, List<HeapEntity> nodes) {
        this.root = root;
        this.nodes = nodes;
    }
}

