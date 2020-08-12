package edu.caltech.cms.intelliviz.graph.logicalvisualization.actions;

import edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers.SimpleTrieVisualization;

public class ToggleTrieAction extends VisualizationToggler {
    @Override
    public Class[] getVisualizers() {
        return new Class[] { SimpleTrieVisualization.class };
    }
}
