package edu.caltech.cms.intelliviz.graph;


import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public interface INode extends Targetable {

    void setPos(double x, double y);
    void draw(Graphics2D g);
    boolean contains(double x, double y);

    double getWidth();
    double getHeight();

    double getX();
    double getY();

    // Target and origin for edges
    Point2D getOrigin(GraphEdge edge);

    ArrayList<GraphEdge> getChildren();

}
