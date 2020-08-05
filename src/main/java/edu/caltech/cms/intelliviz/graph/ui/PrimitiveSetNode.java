package edu.caltech.cms.intelliviz.graph.ui;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.INode;
import org.w3c.dom.css.Rect;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PrimitiveSetNode extends AbstractSetNode {

    private Map<String, Rectangle2D> data = new HashMap<>();

    private static final Font boldItalic = new Font("SanSerif", Font.BOLD | Font.ITALIC, 12);
    private static final int INNER_PADDING = 5;
    private static final int MIN_WIDTH = 40;

    public PrimitiveSetNode() {
        super();
    }

    public void setData(Set<String> data) {
        this.rects.clear();
        this.data.clear();
        for (String s : data) {
            Rectangle2D.Double rect = new Rectangle2D.Double();
            this.data.put(s, rect); // calculate dimensions later....
            this.rects.add(rect);
        }
    }

    @Override
    Color getColor() {
        return GREEN;
    }

    @Override
    void beforeDraw(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setFont(boldItalic);

        FontMetrics fm = g2d.getFontMetrics();
        rects.clear();
        for (Map.Entry<String, Rectangle2D> entry : this.data.entrySet()) {
            double width = fm.stringWidth(entry.getKey()) + 2 * INNER_PADDING;
            width = Math.max(width, MIN_WIDTH);
            double height = fm.getAscent() + 2 * INNER_PADDING;
            entry.getValue().setRect(0, 0, width, height);
            rects.add(entry.getValue());
        }
    }

    @Override
    void afterDraw(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(Color.BLACK);
        g2d.setFont(boldItalic);
        for (Map.Entry<String, Rectangle2D> entry : data.entrySet()) {
            g2d.drawString(entry.getKey(), (int)(entry.getValue().getX() + INNER_PADDING),
                    (int)(entry.getValue().getY() + entry.getValue().getHeight() - INNER_PADDING));
        }
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        return new Point2D.Double(this.x, this.y);
    }

    @Override
    public ArrayList<GraphEdge> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public void highlightChanges(INode ref) {

    }
}
