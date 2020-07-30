package edu.caltech.cms.intelliviz.graph.ui;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.INode;
import org.w3c.dom.css.Rect;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class PrimitiveSetNode extends AbstractSetNode {

    private Map<Rectangle2D, String> data;

    private static final Font boldItalic = new Font("SanSerif", Font.BOLD | Font.ITALIC, 12);
    private static final Color LOWER_COLOR = Color.decode("#C8FAD8");
    private static final int INNER_PADDING = 3;

    public PrimitiveSetNode() {
        super();
    }

    public void setData(Set<String> data) {
        for (String s : data) {
            this.data.put(new Rectangle2D.Double(), s); // calculate dimensions later....
        }
    }

    @Override
    Color getColor() {
        return LOWER_COLOR;
    }

    @Override
    void beforeDraw(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setFont(boldItalic);

        FontMetrics fm = g2d.getFontMetrics();
        for (Map.Entry<Rectangle2D, String> entry : this.data.entrySet()) {
            double width = fm.stringWidth(entry.getValue()) + 2 * INNER_PADDING;
            double height = fm.getAscent() + 2 * INNER_PADDING;
            entry.getKey().setRect(0, 0, width, height);
        }
    }

    @Override
    void afterDraw(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();
        for (Rectangle2D rect : rects) {
            String text = data.get(rect);
            g2d.drawString(text, (int)(rect.getX() + INNER_PADDING), (int)(rect.getY() + rect.getHeight() - INNER_PADDING));
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
