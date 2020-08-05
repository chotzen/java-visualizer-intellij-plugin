package edu.caltech.cms.intelliviz.graph.ui;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.INode;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StackNode implements INode {

    private int     x = 0,
                    y = 0,
                    width = 0,
                    height = ROW_HEIGHT;

    public List<String> primData;
    public List<GraphEdge> pointers;

    private static final int ROW_HEIGHT = 20;
    private static final int MIN_WIDTH = 40;
    private static final int PADDING = 3;

    private boolean highlightHead = false;

    @Override
    public void setPos(double x, double y) {
        this.x = (int)x;
        this.y = (int)y;
    }

    @Override
    public void draw(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();
        int size = (primData != null ? primData.size() : pointers.size());

        this.width = MIN_WIDTH;
        if (primData != null) {
            g2d.setFont(boldItalic);
            primData.forEach(str -> this.width = Math.max(g2d.getFontMetrics().stringWidth(str) + 2 * PADDING, this.width));
        }
        this.height = size * ROW_HEIGHT;

        g2d.setColor(primData == null ? YELLOW : GREEN);
        g2d.fillRect(this.x, this.y, this.width, this.height);

        if (highlightHead) {
            g2d.setColor(INode.HIGHLIGHTED_COLOR);
            g2d.fillRect(this.x, this.y, this.width, ROW_HEIGHT);
        }

        for (int i = 0; i < size; i++) {
            g2d.setColor(Color.BLACK);
            g2d.drawRect(this.x, this.y + i * ROW_HEIGHT, this.width, ROW_HEIGHT);

            if (primData != null) {
                g2d.drawString(primData.get(i), this.x + PADDING, this.y + i * ROW_HEIGHT + g2d.getFontMetrics().getAscent());
            }
        }
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
    public Point2D getOrigin(GraphEdge edge) {
        int idx = this.pointers.indexOf(edge);
        return new Point2D.Double(this.x + this.width / 2d, this.y + (idx + 0.5) * ROW_HEIGHT); // shouldn't happen
    }

    @Override
    public Point2D getTarget(double originX, double originY) {
        return new Point2D.Double(this.x, this.y + ROW_HEIGHT / 2d);
    }

    @Override
    public List<GraphEdge> getChildren() {
        if (this.pointers != null) {
            return this.pointers;
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public void highlightChanges(INode ref) {
        if (ref instanceof StackNode) {
            if (this.primData != null && ((StackNode) ref).primData != null) {
                StackNode snRef = (StackNode) ref;
                this.highlightHead = snRef.primData.get(0).equals(this.primData.get(0)) && snRef.primData.size() == this.primData.size();
            }
        }
    }
}
