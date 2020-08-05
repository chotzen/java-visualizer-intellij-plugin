package edu.caltech.cms.intelliviz.graph.ui;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ObjectSetNode extends AbstractSetNode {

    private Map<GraphEdge, Rectangle2D> pointerMap = new HashMap<>();

    private static final int BOX_WIDTH = 40;
    private static final int BOX_HEIGHT = 28;


    public void setPointers(Set<GraphEdge> pointers) {
        this.pointerMap.clear();
        for (GraphEdge p : pointers) {
            pointerMap.put(p, new Rectangle2D.Double(0, 0, BOX_WIDTH, BOX_HEIGHT));
            this.rects.add(pointerMap.get(p));
        }
    }

    @Override
    Color getColor() {
        return GREEN;
    }

    @Override
    void beforeDraw(Graphics2D g) {
        // do nothing. we already know all the dimensions!
    }

    @Override
    void afterDraw(Graphics2D g) {
        // do nothing
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        Rectangle2D rect = this.pointerMap.get(edge);
        return new Point2D.Double(rect.getCenterX(), rect.getCenterY());
    }

    @Override
    public ArrayList<GraphEdge> getChildren() {
        return new ArrayList<>(this.pointerMap.keySet());
    }

    @Override
    public void highlightChanges(Node ref) {
        // don't do anything
    }
}
