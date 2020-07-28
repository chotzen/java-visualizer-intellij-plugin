package edu.caltech.cms.intelliviz.graph.logical_visualizations;

import com.aegamesi.java_visualizer.model.ExecutionTrace;
import com.aegamesi.java_visualizer.model.HeapEntity;
import edu.caltech.cms.intelliviz.graph.INode;

public interface LogicalVisualization {

    enum DetectionType {
        ON_DETECT, // when the builder first reaches the class
        ON_BUILD  // when nodes and edges have already been built (afterwards)
    }

    String getDisplayName();
    GraphStruct getVisualization(INode root);
    boolean isApplicable(INode root);
    DetectionType getDetectionType();

}
