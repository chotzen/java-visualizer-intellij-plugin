package edu.caltech.cms.intelliviz.graph.ui;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

public class PrimitiveMapNode extends Node {

    private static final int MAX_RENDER_LENGTH = 50;
    private Map<String, String> data;
    private Set<String> highlightedKeys = new HashSet<>();

    private static final int TEXT_PADDING = 4;
    private static final int MIN_COL_WIDTH = 40;
    private static final int ROW_HEIGHT = 30;

    public PrimitiveMapNode(int x, int y) {
        this.x = x;
        this.y = y;
    }

    private int calculateMinWidth(Collection<String> strings, FontMetrics fm) {
        int minWidth = MIN_COL_WIDTH;
        for (String s : strings) {
            if (fm.stringWidth(s) + 2 * TEXT_PADDING > minWidth) {
                minWidth = fm.stringWidth(s) + 2 * TEXT_PADDING;
            }
        }
        return minWidth;
    }

    @Override
    public void draw(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setFont(boldItalic);
        g.setStroke(new BasicStroke(1));
        int keyWidth = calculateMinWidth(data.keySet(), g2d.getFontMetrics());
        int valWidth = calculateMinWidth(data.values(), g2d.getFontMetrics());
        this.width = keyWidth + valWidth;
        this.height = (data.size() + 1) * ROW_HEIGHT;

        drawRow(g2d, 0, "Key", "Value", keyWidth, valWidth, Color.WHITE);
        int row = 1;
        for (String s : data.keySet()) {
            drawRow(g2d, row, s, data.get(s), keyWidth, valWidth, GREEN);
            row++;
        }
    }

    private void drawRow(Graphics2D g, int num, String left, String right, int leftWidth, int rightWidth, Color color) {
        g.setColor(highlightedKeys.contains(left) ? HIGHLIGHTED_COLOR : color);
        g.fillRect((int)x, (int)y + num * ROW_HEIGHT, (int)this.width, ROW_HEIGHT);
        g.setColor(Color.BLACK);
        g.drawRect((int)x, (int)y + num * ROW_HEIGHT, leftWidth, ROW_HEIGHT);
        g.drawRect((int)x + leftWidth, (int)y + num * ROW_HEIGHT, rightWidth, ROW_HEIGHT);

        int textY = (int)(y + (num + 0.5) * ROW_HEIGHT + 0.4 *  g.getFontMetrics().getAscent());
        int lTextX = (int)(x + leftWidth / 2 - g.getFontMetrics().stringWidth(left) / 2);
        int rTextX = (int)(x + leftWidth + rightWidth / 2 - g.getFontMetrics().stringWidth(right) / 2);

        g.drawString(left, lTextX, textY);
        g.drawString(right, rTextX, textY);
    }

    @Override
    public Point2D getTarget(double originX, double originY) {
        return GraphEdge.getCenterTargetingProjection(this,originX, originY);
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
        highlightedKeys.clear();
        if (ref instanceof PrimitiveMapNode) {
            for (Map.Entry<String, String> ent : this.data.entrySet()) {
                if (!ent.getValue().equals(((PrimitiveMapNode)ref).data.get(ent.getKey()))) {
                    highlightedKeys.add(ent.getKey());
                }
            }
        }
    }

    public void setData(Map<String, String> data) {
        Node.warnOnClip(data.size(), MAX_RENDER_LENGTH);
        this.data = Node.clipToLength(data, MAX_RENDER_LENGTH);
    }

    public Map<String, String> getData() {
        return this.data;
    }
}
