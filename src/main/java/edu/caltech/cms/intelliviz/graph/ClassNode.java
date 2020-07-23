package edu.caltech.cms.intelliviz.graph;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

public class ClassNode implements INode {

    private double x, y, width, height;
    public String name;
    private String displayName;
    private HashMap<String, String> fields;
    private ArrayList<GraphEdge> pointers;
    private Set<String> highlightedFields = new HashSet<>();

    private Rectangle2D upper, lower;

    private static final int MIN_WIDTH = 60;
    private static final int HEADER_HEIGHT = 20;
    private static final int TEXT_PADDING = 2;
    private static final Font boldFont = new Font("SanSerif", Font.BOLD, 12);
    private static final Font boldItalic = new Font("SanSerif", Font.BOLD | Font.ITALIC, 12);
    private static final Font normal = new Font("SanSerif", Font.PLAIN, 12);

    private static final Color UPPER_COLOR = Color.decode("#FAF1C8");
    private static final Color LOWER_COLOR = Color.decode("#C8FAD8");

    public ClassNode(double x, double y, String name, HashMap<String, String> fields) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.fields = fields;
        this.pointers = new ArrayList<>();
        upper = new Rectangle2D.Double(x, y, HEADER_HEIGHT, MIN_WIDTH);
        lower = new Rectangle2D.Double(x, y + HEADER_HEIGHT, HEADER_HEIGHT, MIN_WIDTH);
    }

    @Override
    public void setPos(double x, double y) {
        this.x = x;
        this.y = y;
    }

    private void updateRectangles(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();
        String[] pieces = this.name.split("\\.");
        this.displayName = pieces[pieces.length - 1];

        g2d.setFont(boldFont);
        Double[] widths = new Double[1 + fields.size()];
        widths[0] = (double)g2d.getFontMetrics().stringWidth(this.displayName);

        Object[] keys = fields.keySet().toArray();
        for (int i = 1; i <= fields.size(); i++) {
            g2d.setFont(normal);
            widths[i] = (double)g2d.getFontMetrics().stringWidth(keys[i - 1].toString() + ": ");
            g2d.setFont(boldItalic);
            widths[i] += g2d.getFontMetrics().stringWidth(fields.get(keys[i - 1].toString()) + "\"\"");
        }

        double maxWidth = Collections.max(Arrays.asList(widths)) + 2 * TEXT_PADDING;
        this.width = Math.max(MIN_WIDTH, maxWidth);
        this.height = (1 + this.fields.size()) * HEADER_HEIGHT;
        upper.setFrame(x, y, this.width, HEADER_HEIGHT);
        lower.setFrame(x, y + HEADER_HEIGHT, this.width, this.fields.size() * HEADER_HEIGHT);
    }

    void init() {
        this.height = (1 + this.fields.size()) * HEADER_HEIGHT;
    }

    @Override
    public void draw(Graphics2D g) {
        Graphics2D g2d = (Graphics2D) g.create();

        // Draw Rectangles
        updateRectangles(g2d);
        g2d.setColor(UPPER_COLOR);
        g2d.fill(upper);
        g2d.setColor(LOWER_COLOR);
        g2d.fill(lower);

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
        g2d.draw(upper);
        g2d.draw(lower);

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
        return upper.contains(x, y) || lower.contains(x, y);
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
        return GraphEdge.getCenterTargetingProjection(this, originX, originY);
    }

    @Override
    public Point2D getOrigin(GraphEdge edge) {
        return new Point2D.Double(this.x + this.width / 2, this.y + this.height / 2);
    }

    @Override
    public ArrayList<GraphEdge> getChildren() {
        return pointers;
    }

    @Override
    public void highlightChanges(INode ref) {
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

        INode.checkReferencesForTypeChange(this, ref);
    }

    public void addPointer(GraphEdge e) {
        this.pointers.add(e);
    }

}
