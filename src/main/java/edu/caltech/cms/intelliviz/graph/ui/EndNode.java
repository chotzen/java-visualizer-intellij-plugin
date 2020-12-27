package edu.caltech.cms.intelliviz.graph.ui;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class EndNode extends Node {
    private final String text2 = "Click here to show more";

    private int TEXT_PADDING = 4;

    private static final Color BG_COLOR = Color.decode("#FF8F87");
    private static final Color ACCENT_COLOR = Color.decode("#5E0600");

    public EndNode(int x, int y) {
        this.x = x;
        this.y = y;
        this.height = 40;
    }

    @Override
    public void draw(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(BG_COLOR);
        g2d.setFont(scanner.deriveFont(12.0F));
        FontMetrics fm = g2d.getFontMetrics();

        String text1 = "Visualization stopped (too many nodes)";
        this.width = fm.stringWidth(text1) + 2 * TEXT_PADDING;

        Rectangle2D.Double rect = new Rectangle2D.Double(this.x, this.y, this.width, this.height);
        g2d.fill(rect);
        g2d.setColor(ACCENT_COLOR);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(rect);
        g2d.drawString(text1, (int)this.x + TEXT_PADDING, (int)this.y + (int)this.height - TEXT_PADDING - 20);
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD));
        g2d.drawString(text2, (int)this.x + TEXT_PADDING, (int)this.y + (int)this.height - TEXT_PADDING);
    }

    @Override
    public Point2D getTarget(double originX, double originY) {
        return GraphEdge.getCenterTargetingProjection(this, originX, originY);
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        return new Point2D.Double(this.x + this.width, this.y + this.height / 2.0);
    }

    @Override
    public ArrayList<GraphEdge> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public void highlightChanges(Node ref) {

    }

}
