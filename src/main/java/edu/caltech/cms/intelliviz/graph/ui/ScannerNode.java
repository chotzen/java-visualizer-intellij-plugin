package edu.caltech.cms.intelliviz.graph.ui;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.OptionalDouble;

public class ScannerNode extends Node {

    private String contents;
    private int position;
    private boolean closed;

    private static final int PADDING = 3;

    private static final char POSITION_CHAR = '\u2588'; // I think this is a solid rectangle. we'll see.

    public ScannerNode(String contents, int position, boolean closed) {
        this.contents = contents;
        this.position = position;
        this.closed = closed;
    }

    @Override
    public void draw(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();
        String[] rows = convert().split("\n");
        g2d.setFont(scanner);
        OptionalDouble maxWidth = Arrays.stream(rows).mapToDouble(str -> g2d.getFontMetrics().stringWidth(str)).max();
        if (maxWidth.isPresent()) {
            this.height = 2 * PADDING + g2d.getFontMetrics().getAscent() * convert().split("\n").length;
            this.width = 2 * PADDING + (int)maxWidth.getAsDouble();

            if (closed)
                g2d.setColor(Color.LIGHT_GRAY);
            else
                g2d.setColor(Color.WHITE);
            g2d.fillRect(this.x, this.y, this.width, this.height);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(this.x, this.y, this.width, this.height);

            if (closed)
                g2d.setColor(Color.DARK_GRAY);

            int offset = 0;
            for (String s : rows) {
                g2d.drawString(s, this.x + PADDING, this.y + (offset += g2d.getFontMetrics().getAscent()));
            }
        }
    }

    private String convert() {
        return contents.substring(0, position) + POSITION_CHAR + contents.substring(position);
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
    public void highlightChanges(Node ref) {
        // not needed
    }

    @Override
    public Point2D getTarget(double originX, double originY) {
        return GraphEdge.getCenterTargetingProjection(this, originX, originY);
    }
}
