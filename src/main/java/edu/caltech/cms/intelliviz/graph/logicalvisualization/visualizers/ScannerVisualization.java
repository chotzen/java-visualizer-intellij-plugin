package edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import edu.caltech.cms.intelliviz.graph.INode;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.LogicalVisualization;

import java.util.Map;

public class ScannerVisualization extends LogicalVisualization {

    public ScannerVisualization(Map<String, Map<String, String>> classParams, Map<String, Map<String, String>> interfaceParams) {
        super(classParams, interfaceParams);
    }

    @Override
    protected String getDisplayName() {
        return null;
    }

    @Override
    protected void applyOnTrace(ObjectReference ref, ThreadReference thread) {

    }

    @Override
    protected void applyOnBuild(INode ref) {

    }
}
