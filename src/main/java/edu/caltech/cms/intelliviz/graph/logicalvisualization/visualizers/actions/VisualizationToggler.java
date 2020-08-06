package edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.LogicalVisualization;
import org.jetbrains.annotations.NotNull;

public abstract class VisualizationToggler extends ToggleAction {

    public abstract Class[] getVisualizers();

    private boolean toggled = false;

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return toggled;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        toggled = state;
    }
}

