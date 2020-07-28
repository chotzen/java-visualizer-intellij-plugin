package edu.caltech.cms.intelliviz.graph.logical_visualizations;

import edu.caltech.cms.intelliviz.graph.ClassNode;
import edu.caltech.cms.intelliviz.graph.INode;

public class MapVisualization implements LogicalVisualization {

    @Override
    public String getDisplayName() {
        return "Map";
    }

    @Override
    public GraphStruct getVisualization(INode root) {
        return null;
    }

    @Override
    public boolean isApplicable(INode root) {
        return root instanceof ClassNode && ((ClassNode)root).name.equals("HashMap");
    }

    @Override
    public DetectionType getDetectionType() {
        return DetectionType.ON_DETECT;
    }
}
