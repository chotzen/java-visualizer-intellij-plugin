package edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers.MapVisualization;
import org.jetbrains.annotations.NotNull;

public class ToggleMapsAction extends VisualizationToggler {

    @Override
    public Class[] getVisualizers() {
        return new Class[]{MapVisualization.class};
    }
}
