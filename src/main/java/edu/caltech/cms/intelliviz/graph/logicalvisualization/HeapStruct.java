package edu.caltech.cms.intelliviz.graph.logicalvisualization;

import com.aegamesi.java_visualizer.model.HeapEntity;

import java.util.List;

public class HeapStruct {
    public HeapEntity root;
    public List<HeapEntity> nodes;

    public HeapStruct(HeapEntity root, List<HeapEntity> nodes) {
        this.root = root;
        this.nodes = nodes;
    }
}

