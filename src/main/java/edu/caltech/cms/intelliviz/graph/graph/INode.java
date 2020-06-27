package edu.caltech.cms.intelliviz.graph.graph;


import java.awt.*;
import java.awt.geom.Point2D;

public interface INode {
    void setPos(double x, double y);
    void draw(Graphics2D g);
    boolean contains(double x, double y);

    double getWidth();
    double getHeight();

    double getX();
    double getY();

    // Target and origin for edges
    Point2D getTarget(double originX, double originY);
    Point2D getOrigin(GraphEdge edge);
}
