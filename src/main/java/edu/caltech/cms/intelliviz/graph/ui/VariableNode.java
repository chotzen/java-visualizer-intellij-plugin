package edu.caltech.cms.intelliviz.graph.ui;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class VariableNode extends Node {

    public Node reference;
    public String name;
    public String declaringType;
    private String label;
    public boolean isStatic;
    private static final Font font = new Font("Monospaced", Font.BOLD, 13);
    private static final float dash1[] = {5.0f};
    private static final BasicStroke stroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);
    private static final int DD_PADDING = 2;


    public VariableNode(int x, int y, String label, Node reference, String declaringType, boolean isStatic) {
        this.x = x;
        this.y = y;
        this.label = label + " =";
        this.name = label;
        this.reference = reference;
        this.declaringType = declaringType;
        this.isStatic = isStatic;

        // Guess height/width before first display tick
        this.width = 40;
        this.height = 20;
    }

    @Override
    public void draw(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();

        // Draw text
        g2d.setFont(font);
        g2d.setColor(Color.BLACK);

        this.height = g.getFontMetrics().getAscent();
        this.width = g.getFontMetrics(font).stringWidth(this.label);

        g2d.drawString(label, (int)x, (int)y);

        // Draw dashed line
        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setStroke(stroke);
        g2d.setColor(Color.GRAY);
        Point2D origin = getOrigin(null);
        Point2D target = reference.getTarget(origin.getX(), origin.getY());
        g2d.drawLine((int)origin.getX(), (int)origin.getY(), (int)target.getX(), (int)target.getY());
    }

    @Override
    public boolean contains(double x, double y) {
        return new Rectangle2D.Double(this.x - 2 * DD_PADDING, this.y - DD_PADDING - this.height, this.width + 6 * DD_PADDING, this.height + 2 * DD_PADDING)
                .contains(x, y);
    }

    @Override
    public double getWidth() {
        return this.width + 2 * DD_PADDING;
    }

    @Override
    public double getHeight() {
        return this.height + 2 * DD_PADDING;
    }

    @Override
    public Point2D getTarget(double originX, double originY) {
        return new Point2D.Double(this.x, this.y + this.height / 2);
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        return new Point2D.Double(this.x + this.width + 4 * DD_PADDING, this.y - this.height / 4);
    }

    @Override
    public ArrayList<GraphEdge> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public void highlightChanges(Node ref) {
        //INode.checkReferencesForTypeChange(this, ref);
    }
}
