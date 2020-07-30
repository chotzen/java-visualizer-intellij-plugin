package edu.caltech.cms.intelliviz.graph.ui;

import edu.caltech.cms.intelliviz.graph.INode;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public abstract class AbstractSetNode implements INode {

    protected ArrayList<Rectangle2D> rects = new ArrayList<>();
    protected int x = 100, y = 100;
    private int width = 0, height = 0;
    private int upperHeight = 0, lowerHeight = 0;

    private static final int BOX_PADDING = 3;

    public AbstractSetNode() {

    }

    abstract Color getColor();
    abstract void beforeDraw(Graphics2D g);
    abstract void afterDraw(Graphics2D g);

    @Override
    public void setPos(double x, double y) {
        this.x = (int)x;
        this.y = (int)y;
    }

    @Override
    public void draw(Graphics2D g) {
        // this finds widths of boxes based on text, etc.
        beforeDraw(g);

        // figure out what max width/height are
        this.width = BOX_PADDING;
        for (int i = 0; i < Math.ceil(rects.size() / 2.0); i++) {
            double maxWidth;
            if (2*i + 1 < rects.size()) {
                maxWidth = Math.max(rects.get(2*i).getWidth(), rects.get(2*i + 1).getWidth());
                double bottomHeight = (int)rects.get(2*i + 1).getHeight();
                lowerHeight = (int)Math.max(bottomHeight, lowerHeight);
            } else {
                maxWidth = rects.get(2 * i).getWidth();
            }
            upperHeight = (int)Math.max(rects.get(2 * i).getHeight(), upperHeight);
            this.width += maxWidth + BOX_PADDING;
        }
        this.height = 3 * BOX_PADDING + upperHeight + lowerHeight;

        // draw big box
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setColor(Color.WHITE);
        g2d.fillRect(this.x, this.y, this.width, this.height);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(this.x, this.y, this.width, this.height);

        g2d.setColor(getColor());

        // set positions of rectangles
        // fill them with color
        // call sub.draw() to finish rendering

        double xBound = this.x + BOX_PADDING;
        for (int i = 0; i < Math.ceil(rects.size() / 2.0); i++) {
            rects.get(2 * i).setRect(xBound, this.y + BOX_PADDING, rects.get(2 * i).getWidth(), rects.get(2 * i).getHeight());
            g2d.fill(rects.get(2*i));
            if (2 * i + 1 < rects.size()) {
                rects.get(2 * i + 1).setRect(xBound, this.y + 2 * BOX_PADDING + this.upperHeight, rects.get(2 * i + 1).getWidth(), rects.get(2 * i + 1).getHeight());
                g2d.fill(rects.get(2*i + 1));
                xBound += Math.max(rects.get(2*i + 1).getWidth(), rects.get(2*i).getWidth()) + BOX_PADDING;
            }
        }

        afterDraw(g2d);
    }

    @Override
    public boolean contains(double x, double y) {
        return new Rectangle2D.Double(this.x, this.y, this.width, this.height).contains(x, y);
    }

    @Override
    public double getWidth() {
        return this.width;
    }

    @Override
    public double getHeight() {
        return this.height;
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
        return new Point2D.Double(this.x, this.y + this.height / 2.0);
    }
}
