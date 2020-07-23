package edu.caltech.cms.intelliviz.graph;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class NullNode implements INode {

    private double x = 0;
    private double y = 0;
    private double HEIGHT = 15;
    private double WIDTH = 35;
    private boolean highlighted = false;

    private String type;

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
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(highlighted ? HIGHLIGHTED_COLOR : bg);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();

        Rectangle2D.Double rect = new Rectangle2D.Double(this.x, this.y, this.WIDTH, this.HEIGHT);
        g2d.fill(rect);
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(rect);
        g2d.drawString("null", (int)this.x + 3, (int)this.y + (int)this.HEIGHT - 3);

    }

    @Override
    public boolean contains(double x, double y) {
        return new Rectangle2D.Double(this.x, this.y, this.WIDTH, this.HEIGHT).contains(x, y);
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
    public double getX() {
        return this.x;
    }

    @Override
    public double getY() {
        return this.y;
    }

    @Override
    public Point2D getTarget(double originX, double originY) {
        return GraphEdge.getCenterTargetingProjection(this, originX, originY);
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        return new Point2D.Double(this.x + this.WIDTH / 2, this.y + this.HEIGHT / 2);
    }

    @Override
    public ArrayList<GraphEdge> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public void highlightChanges(INode ref) {
        highlighted = true;
    }
}
