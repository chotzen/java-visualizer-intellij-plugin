package edu.caltech.cms.intelliviz.graph;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class NullNode extends INode {

    private double x = 0;
    private double y = 0;
    private double HEIGHT = 15;
    private double WIDTH = 30;

    private Color bg = Color.decode("#DDDDDD");
    private Font font = new Font("Monospaced", Font.PLAIN, 12);

    public NullNode() {

    }

    @Override
    public void setPos(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void draw(Graphics2D g) {

    }

    @Override
    public boolean contains(double x, double y) {
        return new Rectangle2D.Double(this.x, this.y, this.WIDTH, this.HEIGHT).contains(x, y);
    }

    @Override
    public double getWidth() {
        return 0;
    }

    @Override
    public double getHeight() {
        return 0;
    }

    @Override
    public double getX() {
        return this.x;
    }

    @Override
    public double getY() {
        return this.y;
    }

    @Override
    public Point2D getTarget(double originX, double originY) {
        return null;
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        return null;
    }

    @Override
    public ArrayList<GraphEdge> getChildren() {
        return new ArrayList<>();
    }
}
