package edu.caltech.cms.intelliviz.graph.ui;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class CodeNode extends Node {

    private String label;

    private int TEXT_PADDING = 4;

    private static final Color ACTIVE_COLOR = Color.decode("#B9C8D2");

    public StackFrame holeDest = null;

    public CodeNode(int x, int y, String label) {
        this.x = x;
        this.y = y;
        this.label = label;
        this.height = 20;
    }

    @Override
    public void draw(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();
        if (holeDest != null) {
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(this.x + this.width / 2, this.y + this.height / 2, (int)holeDest.getTarget(0, 0).getX(), (int)holeDest.getTarget(0, 0).getY());
        }
        g2d.setColor(ACTIVE_COLOR);
        g2d.setFont(scanner.deriveFont(12.0F));
        FontMetrics fm = g2d.getFontMetrics();

        this.width = fm.stringWidth(this.label) + 2 * TEXT_PADDING;

        Rectangle2D.Double rect = new Rectangle2D.Double(this.x, this.y, this.width, this.height);
        g2d.fill(rect);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(rect);
        g2d.drawString(label, (int)this.x + TEXT_PADDING, (int)this.y + (int)this.height - TEXT_PADDING);

    }

    @Override
    public Point2D getTarget(double originX, double originY) {
        return GraphEdge.getCenterTargetingProjection(this, originX, originY);
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        return new Point2D.Double(this.x + this.width, this.y + this.height / 2);
    }

    @Override
    public ArrayList<GraphEdge> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public void highlightChanges(Node ref) {

    }
}
