package edu.caltech.cms.intelliviz.graph.logicalvisualizations;

import edu.caltech.cms.intelliviz.graph.INode;

public interface LogicalVisualization {

    String getDisplayName();
    GraphStruct getVisualization(INode root);
    boolean isApplicable(INode root);
}
