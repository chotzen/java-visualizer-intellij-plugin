package edu.caltech.cms.intelliviz.graph;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ObjectMapNode implements INode {

    private double x, y;
    private double width, height;
    private Map<String, GraphEdge> data;
    private Map<GraphEdge, Integer> rowMap;

    private static final Color headerColor = Color.WHITE;
    private static final Color lowerColor = Color.decode("#FAF1C8");

    private static final int TEXT_PADDING = 4;
    private static final int MIN_COL_WIDTH = 40;
    private static final int POINTER_COL_WIDTH = 40;
    private static final int ROW_HEIGHT = 30;
    private static final int MAX_RENDER_LENGTH = 50;

    private static final Font font = new Font("SanSerif", Font.BOLD | Font.ITALIC, 12);

    public ObjectMapNode(double x, double y) {
        this.x = x;
        this.y = y;
        rowMap = new HashMap<GraphEdge, Integer>();
    }

    @Override
    public void setPos(double x, double y) {
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
        g2d.setFont(font);
        g.setStroke(new BasicStroke(1));
        int keyWidth = calculateMinWidth(data.keySet(), g2d.getFontMetrics());
        this.width = keyWidth + POINTER_COL_WIDTH;
        this.height = (data.size() + 1) * ROW_HEIGHT;

        drawRow(g2d, 0, "Key", "Value", keyWidth, POINTER_COL_WIDTH, headerColor);
        int row = 1;
        rowMap.clear();
        for (String s : data.keySet()) {
            drawRow(g2d, row, s, "", keyWidth, POINTER_COL_WIDTH, lowerColor);
            rowMap.put(data.get(s), row);
            row++;
        }
    }

    private void drawRow(Graphics2D g, int num, String left, String right, int leftWidth, int rightWidth, Color color) {
        g.setColor(color);
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
    public Point2D getTarget(double originX, double originY) {
        return GraphEdge.getCenterTargetingProjection(this,originX, originY);
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        int row = rowMap.get(edge);
        return new Point2D.Double(x + this.width - POINTER_COL_WIDTH / 2d, y + ROW_HEIGHT * (row + 0.5));
    }

    @Override
    public ArrayList<GraphEdge> getChildren() {
        return new ArrayList<>(data.values());
    }

    @Override
    public void highlightChanges(INode ref) {
        INode.checkReferencesForTypeChange( this, ref);
    }

    public void setData(Map<String, GraphEdge> data) {
        INode.warnOnClip(this.data.size(), MAX_RENDER_LENGTH);
        this.data = INode.clipToLength(data, MAX_RENDER_LENGTH);
    }
}
