package edu.caltech.cms.intelliviz.graph;


import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

interface INode {

    abstract void setPos(double x, double y);
    abstract void draw(Graphics2D g);
    abstract boolean contains(double x, double y);

    abstract double getWidth();
    abstract double getHeight();

    abstract double getX();
    abstract double getY();

    // Target and origin for edges
    abstract Point2D getTarget(double originX, double originY);
    abstract Point2D getOrigin(GraphEdge edge);

    abstract ArrayList<GraphEdge> getChildren();

}
