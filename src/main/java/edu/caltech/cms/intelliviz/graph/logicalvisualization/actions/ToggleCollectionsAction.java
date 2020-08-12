package edu.caltech.cms.intelliviz.graph.logicalvisualization.actions;

import edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers.ArrayListVisualization;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers.SetVisualization;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers.StackVisualization;

public class ToggleCollectionsAction extends VisualizationToggler {

    private static final Class[] COLLECTION_VISUALIZERS = new Class[]{
            ArrayListVisualization.class,
            SetVisualization.class,
            StackVisualization.class
    };

    @Override
    public Class[] getVisualizers() {
        return COLLECTION_VISUALIZERS;
    }
}

