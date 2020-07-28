package edu.caltech.cms.intelliviz.graph.logicalvisualization;

import edu.caltech.cms.intelliviz.graph.INode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class LogicalVisualization {

    static List<LogicalVisualization> vizList = new ArrayList<>();

    private Set<String> classes     = new HashSet<>(),
                        interfaces  = new HashSet<>();

    abstract String getDisplayName();
    abstract void applyLayout(INode ref);

    // loads the classes to which it shall be applied
    void loadFromCfg() {

    }

    void apply(INode ref) {
        // TODO: get interface data in INode
    }

}

