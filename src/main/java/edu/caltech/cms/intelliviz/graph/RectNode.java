package edu.caltech.cms.intelliviz.graph;


import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class RectNode implements INode {

    private double x, y;
    public String text;

    private Rectangle2D rect;

    private final double WIDTH = 40;
    private final double HEIGHT = 40;

    private final Color COLOR = Color.darkGray;

    public RectNode(double x, double y, String text) {
        this.x = x;
        this.y = y;

        rect = new Rectangle2D.Double(x, y, WIDTH, HEIGHT);
    }

    public void setPos(double x, double y) {
        this.x = x;
        this.y = y;
        rect.setFrame(x, y, WIDTH, HEIGHT);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void draw(Graphics2D g) {
        g.setColor(COLOR);
        g.fill(rect);
    }

    public boolean contains(double x, double y) {
        return rect.contains(x, y);
    }

    @Override
    public double getWidth() {
        return this.WIDTH;
    }

    @Override
    public double getHeight() {
        return this.HEIGHT;
    }

    @Override
    public Point2D getTarget(double originX, double originY) {
        return GraphEdge.getCenterTargetingProjection(this, originX, originY);
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        return new Point2D.Double(this.x + getWidth() / 2, this.y + getHeight() / 2);
    }
}
