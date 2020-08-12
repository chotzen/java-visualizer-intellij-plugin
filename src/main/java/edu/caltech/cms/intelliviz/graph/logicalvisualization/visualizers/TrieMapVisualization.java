package edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers;

import com.aegamesi.java_visualizer.model.HeapObject;

import java.util.Map;

public class TrieMapVisualization extends TrieVisualization {
    public TrieMapVisualization(Map<String, Map<String, String>> classParams, Map<String, Map<String, String>> interfaceParams) {
        super(classParams, interfaceParams);
    }

    @Override
    public void addField(HeapObject obj, String key, com.aegamesi.java_visualizer.model.Value newRef) {
        obj.mapFields.put(key, newRef);
    }
}


