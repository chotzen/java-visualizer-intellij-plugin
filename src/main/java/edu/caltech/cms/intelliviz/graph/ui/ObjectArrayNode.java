package edu.caltech.cms.intelliviz.graph.ui;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

public class ObjectArrayNode extends Node {

    private ArrayList<TextLabel> labels;
    public Map<GraphEdge, Integer> pointers = new HashMap<>();
    public Set<Integer> nullIndices = new HashSet<>();
    private int length;
    private int BOX_WIDTH = 30;
    private int TEXT_PADDING = 3;
    private int BOX_HEIGHT = 30;
    private final int LABEL_VERT_OFFSET = 8;
    private final int LABEL_HORIZ_OFFSET = 2;

    private final int MAX_RENDER_LENGTH = 50;

    private Font font = new Font("Monospaced", Font.PLAIN, 11);

    public ObjectArrayNode(int x, int y, int length) {
        this.x = x;
        this.y = y;
        this.length = length;
        this.height = BOX_HEIGHT;
        labels = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            labels.add(new TextLabel(String.valueOf(i)));
        }

        this.width = length * BOX_WIDTH;
    }

    public void setPointers(Map<GraphEdge, Integer> edges) {
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
            drawCell(g2d, (int)(x + i * BOX_WIDTH), (int)y, BOX_WIDTH, i);
            labels.get(i).draw(g, x + (i + 0.5) * BOX_WIDTH + LABEL_HORIZ_OFFSET, y + BOX_HEIGHT + LABEL_VERT_OFFSET);
        }
    }

    private void drawCell(Graphics2D g, int x, int y, int width, int i) {
        if (nullIndices.contains(i)) {
            g.setColor(Color.WHITE);
        } else {
            g.setColor(YELLOW);
        }
        g.fillRect(x, y, width, BOX_HEIGHT);

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1));
        g.drawRect(x, y, width, BOX_HEIGHT);

        if (nullIndices.contains(i)) {
            g.setFont(font);
            g.drawString("null", x + 2, y + BOX_HEIGHT * 2 / 3);
        }
    }

    @Override
    public Point2D getTarget(double originX, double originY) {
        return new Point2D.Double(this.x, this.y + BOX_HEIGHT / 2d);
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        if (pointers.containsKey(edge)) {
            return new Point2D.Double(x + (pointers.get(edge) + 0.5) * BOX_WIDTH, y + 0.5 * BOX_HEIGHT);
        } else {
            return new Point2D.Double(0, 0);
        }
    }

    @Override
    public ArrayList<GraphEdge> getChildren() {
        return new ArrayList<>(pointers.keySet());
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
