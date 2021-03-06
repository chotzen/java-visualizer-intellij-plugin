package edu.caltech.cms.intelliviz.graph.logicalvisualization.actions;

import com.aegamesi.java_visualizer.plugin.MainPane;
import com.intellij.debugger.engine.DebuggerManagerThreadImpl;
import com.intellij.debugger.engine.managerThread.DebuggerManagerThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import org.jetbrains.annotations.NotNull;

public abstract class VisualizationToggler extends ToggleAction {

    public abstract Class[] getVisualizers();

    public boolean toggled = false;

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return toggled;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        toggled = state;

        MainPane pane = (MainPane) e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
        pane.getManager().traceAndVisualize();
    }
}

