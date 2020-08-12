package edu.caltech.cms.intelliviz.graph.ui;

import com.google.common.graph.Graph;
import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

public class ObjectMapNode extends Node {

    private Map<String, GraphEdge> primKeyData;
    private Map<GraphEdge, GraphEdge> objKeyData;
    private Map<GraphEdge, String> primValData;
    private Map<GraphEdge, Integer> rowMap;

    private static final int TEXT_PADDING = 4;
    private static final int MIN_COL_WIDTH = 40;
    private static final int POINTER_COL_WIDTH = 40;
    private static final int ROW_HEIGHT = 30;
    private static final int MAX_RENDER_LENGTH = 50;

    public ObjectMapNode(int x, int y) {
        this.x = x;
        this.y = y;
        rowMap = new LinkedHashMap<GraphEdge, Integer>();
    }

    @Override
    public void setPos(double x, double y) {
        this.x = (int)x;
        this.y = (int)y;
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
        int keyWidth = calculateMinWidth(primKeyData == null ? Collections.emptyList() : primKeyData.keySet(), g2d.getFontMetrics());
        int valWidth = calculateMinWidth(primValData == null ? Collections.emptyList() : primValData.values(), g2d.getFontMetrics());
        this.width = keyWidth + valWidth;
        if (primKeyData != null) {
            this.height = (primKeyData.size() + 1) * ROW_HEIGHT;
        } else if (objKeyData != null) {
            this.height = (objKeyData.size() + 1) * ROW_HEIGHT;
        } else if (primValData != null) {
            this.height = (primValData.size() + 1) * ROW_HEIGHT;
        }

        drawRow(g2d, 0, "Key", "Value", keyWidth, POINTER_COL_WIDTH, Color.WHITE);
        int row = 1;
        rowMap.clear();
        if (primKeyData != null) {
            for (String s : primKeyData.keySet()) {
                drawRow(g2d, row, s, "", keyWidth, POINTER_COL_WIDTH, YELLOW);
                rowMap.put(primKeyData.get(s), row);
                row++;
            }
        } else if (objKeyData != null) {
            for (GraphEdge e : objKeyData.keySet()) {
                drawRow(g2d, row, "", "", keyWidth, POINTER_COL_WIDTH, YELLOW);
                rowMap.put(objKeyData.get(e), row);
                rowMap.put(e, row);
                row++;
            }
        } else if (primValData != null) {
            for (Map.Entry<GraphEdge, String> entry : primValData.entrySet()) {
                drawRow(g2d, row, "", entry.getValue(), keyWidth, valWidth, YELLOW);
                rowMap.put(entry.getKey(), row);
                row++;
            }
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
    public Point2D getTarget(double originX, double originY) {
        return GraphEdge.getCenterTargetingProjection(this,originX, originY);
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        int row = rowMap.get(edge);
        if (objKeyData != null && objKeyData.containsKey(edge)) {
            return new Point2D.Double(x + MIN_COL_WIDTH / 2d, y + ROW_HEIGHT * (row + 0.5));
        }
        if (primValData != null && primValData.containsKey(edge)) {
            return new Point2D.Double(x + MIN_COL_WIDTH / 2d, y + ROW_HEIGHT * (row + 0.5));
        }
        return new Point2D.Double(x + this.width - POINTER_COL_WIDTH / 2d, y + ROW_HEIGHT * (row + 0.5));
    }

    @Override
    public ArrayList<GraphEdge> getChildren() {
        if (primKeyData != null) {
            return new ArrayList<>(primKeyData.values());
        } else if (primValData != null) {
            return new ArrayList<>(primValData.keySet());
        } else {
            ArrayList<GraphEdge> result = new ArrayList<>(objKeyData.values());
            result.addAll(objKeyData.keySet());
            return result;
        }
    }

    @Override
    public void highlightChanges(Node ref) {
        Node.checkReferencesForTypeChange( this, ref);
    }

    public void setPrimData(Map<String, GraphEdge> data) {
        Node.warnOnClip(data.size(), MAX_RENDER_LENGTH);
        this.primKeyData = Node.clipToLength(data, MAX_RENDER_LENGTH);
        this.objKeyData = null;
        this.primValData = null;
    }

    public void setObjData(Map<GraphEdge, GraphEdge> data) {
        Node.warnOnClip(data.size(), MAX_RENDER_LENGTH);
        this.objKeyData = Node.clipToLength(data, MAX_RENDER_LENGTH);
        this.primKeyData = null;
        this.primValData = null;
    }

    public void setPrimValData(Map<GraphEdge, String> data) {
        Node.warnOnClip(data.size(), MAX_RENDER_LENGTH);
        this.primValData = Node.clipToLength(data, MAX_RENDER_LENGTH);
        this.primKeyData = null;
        this.objKeyData = null;
    }

    @Override
    public RenderBehavior getRenderBehavior() {
        return RenderBehavior.BEFORE_EDGES;
    }
}
