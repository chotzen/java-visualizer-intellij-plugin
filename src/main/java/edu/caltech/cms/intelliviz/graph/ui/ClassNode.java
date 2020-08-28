package edu.caltech.cms.intelliviz.graph.ui;

import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.Node;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

public class ClassNode extends Node {

    public String name;
    public Set<String> implementedInterfaces = new HashSet<>();
    private String displayName;
    public HashMap<String, String> fields;
    public ArrayList<GraphEdge> pointers;
    private Set<String> highlightedFields = new HashSet<>();

    private static final int MIN_WIDTH = 60;
    private static final int TEXT_PADDING = 2;


    public ClassNode(int x, int y, String name, HashMap<String, String> fields) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.fields = fields;
        this.pointers = new ArrayList<>();
        String[] pieces = this.name.split("\\.");
        this.displayName = pieces[pieces.length - 1];
    }

    int maxWidth(Graphics2D g2D) {
       g2D.setFont(boldFont);
       int topWidth = g2D.getFontMetrics().stringWidth(this.displayName);

       OptionalInt maxOtherWidth = this.fields.entrySet().stream().mapToInt((entry) -> {
           g2D.setFont(normal);
           int width = g2D.getFontMetrics().stringWidth(entry.getKey() + ": ");
           g2D.setFont(boldItalic);
           width += g2D.getFontMetrics().stringWidth(entry.getValue() + "\"\"");
           return width;
       }).max();
       if (maxOtherWidth.isPresent()) {
           return Math.max(Math.max(maxOtherWidth.getAsInt(), topWidth), MIN_WIDTH) + 2 * TEXT_PADDING;
       }
       return Math.max(topWidth, MIN_WIDTH) + 2 * TEXT_PADDING;
    }


    @Override
    public void draw(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();
        this.width = maxWidth(g2d);
        this.height = (1 + this.fields.size()) * HEADER_HEIGHT;

        // Draw Rectangles
        g2d.setColor(YELLOW);
        g2d.fillRect(this.x, this.y, this.width, HEADER_HEIGHT);

        g2d.setColor(GREEN);
        g2d.fillRect(this.x, this.y + HEADER_HEIGHT, this.width, this.fields.size() * HEADER_HEIGHT);

        // highlight squares
        int offset = 1;
        g2d.setColor(HIGHLIGHTED_COLOR);
        for (String k : fields.keySet()) {
            if (highlightedFields.contains(k)) {
                g2d.fillRect((int)x, (int)y + HEADER_HEIGHT * offset, (int)this.width, HEADER_HEIGHT);
            }
            offset++;
        }

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        // draw borders
        g2d.drawRect(this.x, this.y, this.width, HEADER_HEIGHT);
        g2d.drawRect(this.x, this.y + HEADER_HEIGHT, this.width, this.fields.size() * HEADER_HEIGHT);



        // Draw header
        g2d.setFont(boldFont);
        int vertOffset = g2d.getFontMetrics().getAscent();
        g2d.drawString(this.displayName, (int)(x + TEXT_PADDING), (int)(y + TEXT_PADDING + vertOffset));

        int count = 1;
        for (String k : fields.keySet()) {
            g2d.setFont(normal);
            String in = k + ": ";
            int keyWidth = g2d.getFontMetrics().stringWidth(in);
            g2d.drawString(in, (int)(x + TEXT_PADDING), (int)(y + TEXT_PADDING + count * HEADER_HEIGHT + vertOffset));
            g2d.setFont(boldItalic);
            g2d.drawString(fields.get(k),  (int)(x + TEXT_PADDING + keyWidth), (int)(y + TEXT_PADDING + count * HEADER_HEIGHT + vertOffset));
            count++;
        }

   }

    @Override
    public boolean contains(double x, double y) {
        return new Rectangle2D.Double(this.x, this.y, this.width, this.height).contains(x, y);
    }

    @Override
    public Point2D getTarget(double originX, double originY) {
        return GraphEdge.getCenterTargetingProjection(this, originX, originY);
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        return new Point2D.Double(this.x + this.width / 2d, this.y + this.height / 2d);
    }

    @Override
    public ArrayList<GraphEdge> getChildren() {
        return pointers;
    }

    @Override
    public void highlightChanges(Node ref) {
        highlightedFields.clear();
        if (ref instanceof ClassNode) {
            ClassNode node = (ClassNode) ref;
            // compare fields to those of this node, and highlight the ones that are different
            for (Map.Entry<String, String> ent : node.fields.entrySet()) {
                if (!ent.getValue().equals(this.fields.get(ent.getKey()))) {
                    this.highlightedFields.add(ent.getKey());
                }
            }
        } else {
            // not sure when this would ever happen
            System.out.println("Hey! IDs can change type. Cool, huh? Bet you didn't expect that.");
        }

        Node.checkReferencesForTypeChange(this, ref);
    }

    public void addPointer(GraphEdge e) {
        this.pointers.add(e);
    }

}
