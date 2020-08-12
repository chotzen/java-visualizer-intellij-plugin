package edu.caltech.cms.intelliviz.graph.ui;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class ObjectArrayNode extends Node {

    private ArrayList<TextLabel> labels;
    public List<GraphEdge> pointers;
    private int length;
    private int BOX_WIDTH = 30;
    private int TEXT_PADDING = 3;
    private int BOX_HEIGHT = 30;
    private final int LABEL_VERT_OFFSET = 8;
    private final int LABEL_HORIZ_OFFSET = 2;

    private final int MAX_RENDER_LENGTH = 50;

    public ObjectArrayNode(int x, int y, int length) {
        this.x = x;
        this.y = y;
        this.pointers = new ArrayList<GraphEdge>();
        this.length = length;
        this.height = BOX_HEIGHT;
        labels = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            labels.add(new TextLabel(String.valueOf(i)));
        }

        this.width = length * BOX_WIDTH;
    }

    public void setPointers(List<GraphEdge> edges) {
        this.pointers = edges;
        Node.warnOnClip(this.pointers.size(), MAX_RENDER_LENGTH);
    }

    @Override
    public void setPos(double x, double y) {
        this.x = (int)x;
        this.y = (int)y;
    }

    @Override
    public void draw(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();
        for (int i = 0; i < length; i++) {
            drawCell(g2d, (int)(x + i * BOX_WIDTH), (int)y, BOX_WIDTH);
            labels.get(i).draw(g, x + (i + 0.5) * BOX_WIDTH + LABEL_HORIZ_OFFSET, y + BOX_HEIGHT + LABEL_VERT_OFFSET);
        }
    }

    private void drawCell(Graphics2D g, int x, int y, int width) {
        g.setColor(YELLOW);
        g.fillRect(x, y, width, BOX_HEIGHT);

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1));
        g.drawRect(x, y, width, BOX_HEIGHT);
    }

    @Override
    public Point2D getTarget(double originX, double originY) {
        return new Point2D.Double(this.x, this.y + BOX_HEIGHT / 2d);
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        for (int i = 0; i < pointers.size(); i++) {
            if (edge.equals(pointers.get(i))) {
                return new Point2D.Double(x + (i + 0.5) * BOX_WIDTH, y + 0.5 * BOX_HEIGHT);
            }
        }
        return null;
    }

    @Override
    public ArrayList<GraphEdge> getChildren() {
        return new ArrayList<>(pointers);
    }

    @Override
    public void highlightChanges(Node ref) {
        Node.checkReferencesForTypeChange( this, ref);
    }

    @Override
    public RenderBehavior getRenderBehavior() {
        return RenderBehavior.BEFORE_EDGES;
    }

}
