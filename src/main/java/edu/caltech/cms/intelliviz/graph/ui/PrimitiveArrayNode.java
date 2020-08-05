package edu.caltech.cms.intelliviz.graph.ui;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.INode;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class PrimitiveArrayNode implements INode {

    private double x, y;

    private ArrayList<TextLabel> labels;
    private String[] values;
    private Set<Integer> highlightedIndices = new HashSet<>();
    private double width;
    private int MIN_BOX_WIDTH = 20;
    private int TEXT_PADDING = 3;
    private int BOX_HEIGHT = 20;
    private final int LABEL_VERT_OFFSET = 8;
    private final int LABEL_HORIZ_OFFSET = 2;

    private final int MAX_RENDER_LENGTH = 200;

    public PrimitiveArrayNode(double x, double y, String[] values) {
        this.x = x;
        this.y = y;
        this.values = values;
        labels = new ArrayList<>();
        for (int i = 0; i < Math.min(values.length, MAX_RENDER_LENGTH); i++) {
            labels.add(new TextLabel(String.valueOf(i)));

        }
        if (values.length > MAX_RENDER_LENGTH) {
            INode.warnOnClip(values.length, MAX_RENDER_LENGTH);
        }

        // Estimate width before it's calculated on draw, as we don't have a Graphics2D here
        this.width = values.length * MIN_BOX_WIDTH;
    }

    @Override
    public void setPos(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void draw(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setFont(boldItalic);
        FontMetrics fm = g2d.getFontMetrics();
        this.width = 0;
        if (this.values.length != 0) {
            for (int i = 0; i < Math.min(values.length, MAX_RENDER_LENGTH); i++) {
                int textWidth = fm.stringWidth(values[i]) + 2 * TEXT_PADDING;
                if (textWidth + 2 * TEXT_PADDING < MIN_BOX_WIDTH) {
                    drawCell(g2d, (int)(x + this.width), (int)y, MIN_BOX_WIDTH, textWidth, values[i], i);
                    labels.get(i).draw(g, x + this.width + 0.5 * MIN_BOX_WIDTH + LABEL_HORIZ_OFFSET, y + BOX_HEIGHT + LABEL_VERT_OFFSET);
                    this.width += MIN_BOX_WIDTH;
                } else {
                    drawCell(g2d, (int)(x + this.width), (int)y, textWidth, textWidth, values[i], i);
                    labels.get(i).draw(g, x + this.width + 0.5 * textWidth + LABEL_HORIZ_OFFSET, y + BOX_HEIGHT + LABEL_VERT_OFFSET);
                    this.width += textWidth;
                }
            }
        } else {
            // draw a black bar because there aren't any elements
            g2d.setColor(Color.BLACK);
            g2d.fillRect((int)x, (int)y, 5, BOX_HEIGHT);
        }
    }

    private void drawCell(Graphics2D g, int x, int y, int width, int textWidth, String text, int idx) {
        g.setColor(highlightedIndices.contains(idx) ? HIGHLIGHTED_COLOR : GREEN);
        g.fillRect(x, y, width, BOX_HEIGHT);

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1));
        g.drawRect(x, y, width, BOX_HEIGHT);

        int text_x = x + width / 2 - textWidth / 2 + TEXT_PADDING;
        int text_y = y + BOX_HEIGHT / 2 + (int)(0.4 * g.getFontMetrics().getAscent());

        g.drawString(text, text_x, text_y);
    }

    @Override
    public boolean contains(double x, double y) {
        return new Rectangle2D.Double(this.x, this.y, this.width, this.BOX_HEIGHT).contains(x, y);
    }

    @Override
    public double getWidth() {
        return this.width;
    }

    @Override
    public double getHeight() {
        return BOX_HEIGHT;
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
        return new Point2D.Double(this.x, this.y + BOX_HEIGHT / 2d);
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        return new Point2D.Double(this.x + this.width, this.y + BOX_HEIGHT / 2d);
    }

    @Override
    public ArrayList<GraphEdge> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public void highlightChanges(INode ref) {
        highlightedIndices.clear();
        if (ref instanceof PrimitiveArrayNode) {
            PrimitiveArrayNode pan = (PrimitiveArrayNode) ref;
            for (int i = 0; i < this.values.length; i++) {
                if (!this.values[i].equals(pan.values[i])) {
                    highlightedIndices.add(i);
                }
            }
        } else {
            // shouldn't happen
            System.out.println("IDs can change reference type!! Whoops");
        }
    }
}
