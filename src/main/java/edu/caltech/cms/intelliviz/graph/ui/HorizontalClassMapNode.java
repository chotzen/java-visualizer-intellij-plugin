package edu.caltech.cms.intelliviz.graph.ui;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

public class HorizontalClassMapNode extends Node {

    public TreeMap<String, GraphEdge> refs;
    public Map<String, String> fields;
    public List<GraphEdge> pointers;
    private Map<GraphEdge, Integer> pointerOffsets = new HashMap<>();
    private Set<String> highlightedFields = new HashSet<>();
    public String label;
    private String displayName;

    private static final int TEXT_PADDING = 2;
    private static final int MIN_PTR_WIDTH = 20;
    private static final int MIN_WIDTH = 50;
    private static final int MAP_HEIGHT = 25;

    public HorizontalClassMapNode(String label) {
        super();
        refs = new TreeMap<>(String::compareTo);
        fields = new HashMap<>();
        pointers = new ArrayList<>();
        this.label = label;
        String[] pieces = label.split("\\.");
        this.displayName = pieces[pieces.length - 1];
    }

    @Override
    public void draw(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setFont(boldItalic);

        int maxFieldWidth = this.fields.entrySet().stream()
                .mapToInt(entr -> calculateFieldWidth(entr, g2d))
                .max()
                .getAsInt();

        int mapWidth = this.refs.keySet().stream()
                .mapToInt(key -> getPtrWidth(key, g2d))
                .sum();
        g2d.setFont(boldFont);
        this.width = Math.max(Math.max(maxFieldWidth, mapWidth), Math.max(MIN_WIDTH, g2d.getFontMetrics().stringWidth(this.displayName) + 3 * TEXT_PADDING));

        this.height = HEADER_HEIGHT * (1 + fields.size()) + 2 * MAP_HEIGHT;

        // draw header
        g2d.setColor(YELLOW);
        Rectangle2D header = new Rectangle2D.Double(this.x, this.y, this.width, HEADER_HEIGHT);
        g2d.fill(header);

        // fields
        g2d.setColor(GREEN);
        Rectangle2D fields = new Rectangle2D.Double(this.x, this.y + HEADER_HEIGHT, this.width, HEADER_HEIGHT * this.fields.size());
        g2d.fill(fields);

        // map bit
        g2d.setColor(YELLOW);
        Rectangle2D mapArea = new Rectangle2D.Double(this.x, this.y + (1 + this.fields.size()) * HEADER_HEIGHT,  this.width, MAP_HEIGHT * 2);
        g2d.fill(mapArea);


        // draw header text
        g2d.setFont(boldFont);
        g2d.setColor(Color.BLACK);
        int offset = g2d.getFontMetrics().getAscent();
        g2d.drawString(this.displayName, this.x + TEXT_PADDING, this.y + TEXT_PADDING + offset);

        // draw field texts
        int count = 1;
        for (Map.Entry<String, String> fieldEntry : this.fields.entrySet()) {
            if (highlightedFields.contains(fieldEntry.getKey())) {
                g2d.setColor(HIGHLIGHTED_COLOR);
                g2d.fillRect(this.x, this.y + count * HEADER_HEIGHT, this.width, HEADER_HEIGHT);
            }
            g2d.setFont(normal);
            g2d.setColor(Color.BLACK);
            String in = fieldEntry.getKey() + ": ";
            int keyWidth = g2d.getFontMetrics().stringWidth(in);
            g2d.drawString(in, x + TEXT_PADDING, y + TEXT_PADDING + count * HEADER_HEIGHT + offset);
            g2d.setFont(boldItalic);
            g2d.drawString(fieldEntry.getValue(), x + TEXT_PADDING + keyWidth, y + TEXT_PADDING + offset + count * HEADER_HEIGHT);

            count++;
        }

        int xOffset = 0;
        int yPos = HEADER_HEIGHT * (1 + this.fields.size());
        g2d.setStroke(new BasicStroke(1));
        for (Map.Entry<String, GraphEdge> entry : this.refs.entrySet()) {
            int boxSize = maxFieldWidth > mapWidth ? (int)(this.width / (double) this.refs.size()) :
                                                getPtrWidth(entry.getKey(), g2d);

            int xPos = this.x + xOffset + (int)(boxSize / 2d) - (int)(getPtrWidth(entry.getKey(), g2d) / 2d) + TEXT_PADDING;
            g2d.drawString(entry.getKey(), xPos, this.y + yPos + g2d.getFontMetrics().getAscent() + TEXT_PADDING);

            // draw box
            g2d.drawRect(this.x + xOffset, this.y + yPos, boxSize, MAP_HEIGHT);
            g2d.drawRect(this.x + xOffset, this.y + yPos + MAP_HEIGHT, boxSize, MAP_HEIGHT);

            pointerOffsets.put(entry.getValue(), xOffset + boxSize / 2);

            xOffset += boxSize;
        }

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(header);
        g2d.draw(fields);
        g2d.draw(mapArea);
    }

    int calculateFieldWidth(Map.Entry<String, String> entr, Graphics2D g2D) {
        g2D.setFont(normal);
        int lWidth = g2D.getFontMetrics().stringWidth(entr.getKey());
        g2D.setFont(boldItalic);
        int rWidth = g2D.getFontMetrics().stringWidth(entr.getValue());
        if (entr.getValue().equals("null")) {
            rWidth += 5;
        }
        return lWidth + rWidth + 4 * TEXT_PADDING;
    }

    int getPtrWidth(String lbl, Graphics2D g2D) {
        g2D.setFont(boldItalic);
        return Math.max(MIN_PTR_WIDTH, g2D.getFontMetrics().stringWidth(lbl) + 2 * TEXT_PADDING);
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        return new Point2D.Double(this.x + pointerOffsets.get(edge), this.y + (1 + this.fields.size()) * HEADER_HEIGHT + MAP_HEIGHT * 1.5);
    }

    @Override
    public Point2D getTarget(double originX, double originY) {
        return GraphEdge.getCenterTargetingProjection(this, originX, originY);
    }

    @Override
    public List<GraphEdge> getChildren() {
        ArrayList<GraphEdge> res = new ArrayList<>(pointers);
        res.addAll(this.refs.values());
        return res;
    }

    @Override
    public void highlightChanges(Node ref) {
        if (ref instanceof HorizontalClassMapNode) {
            HorizontalClassMapNode href = (HorizontalClassMapNode) ref;

            this.highlightedFields.clear();

            for (Map.Entry<String, String> ent : href.fields.entrySet()) {
                if (!ent.getValue().equals(this.fields.get(ent.getKey()))) {
                    this.highlightedFields.add(ent.getKey());
                }
            }
        }
    }

    @Override
    public RenderBehavior getRenderBehavior() {
        return RenderBehavior.BEFORE_EDGES;
    }
}
