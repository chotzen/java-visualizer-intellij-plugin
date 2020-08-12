package edu.caltech.cms.intelliviz.graph.logicalvisualization.actions;

import edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers.MapVisualization;

public class ToggleMapsAction extends VisualizationToggler {

    @Override
    public Class[] getVisualizers() {
        return new Class[]{MapVisualization.class};
    }
}
