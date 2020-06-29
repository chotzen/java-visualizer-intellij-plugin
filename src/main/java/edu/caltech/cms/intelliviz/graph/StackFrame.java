package edu.caltech.cms.intelliviz.graph;

import com.aegamesi.java_visualizer.model.Frame;
import com.aegamesi.java_visualizer.model.HeapEntity;
import com.aegamesi.java_visualizer.model.Value;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.Map;

public class StackFrame {

    private static final int HEIGHT = 20;
    private static final int TEXT_OFFSET = 2;
    private static final Font font = new Font("Monospaced", Font.ITALIC | Font.BOLD, 12);
    private static final Color INACTIVE_COLOR = Color.decode("#B9C8D2");
    private static final Color ACTIVE_COLOR = Color.decode("#DDF5FA");

    private String displayString;
    private int depth;
    private boolean active;


    public StackFrame(Map<Long, HeapEntity> heap, Frame fr, int depth, boolean active) {
        this.depth = depth;
        this.active = active;
        this.displayString = fr.name.split(":")[0] + "(";
        Iterator<String> iter = fr.locals.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            this.displayString += key + " = ";
            if (fr.locals.get(key).type == Value.Type.REFERENCE) {
                long ref = fr.locals.get(key).reference;
                this.displayString += heap.get(ref).label;
            } else {
                String[] s = fr.locals.get(key).toString().split("\\.");
                this.displayString += s[s.length - 1]
                ;
            }

            if (iter.hasNext()) {
                this.displayString += ",";
            }
        }
        this.displayString += ")";
    }

    public void draw(Graphics g, int vertOffset, int width) {
        Graphics2D g2d = (Graphics2D) g.create();
        // Draw rectangle
        int x = (1 + this.depth) * HEIGHT;
        int y = (1 + this.depth) * HEIGHT + vertOffset;
        Rectangle2D box = new Rectangle2D.Double(x, y, width - depth * HEIGHT, HEIGHT);
        if (active) {
            g2d.setColor(ACTIVE_COLOR);
        } else {
            g2d.setColor(INACTIVE_COLOR);
        }
        g2d.fill(box);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(box);

        // Draw text
        g2d.setFont(font);
        g2d.drawString(this.displayString, x + TEXT_OFFSET, y + TEXT_OFFSET + g2d.getFontMetrics().getAscent());
    }

    public Point2D getOrigin(int vertOffset) {
        return new Point2D.Double((1 + this.depth) * HEIGHT, (1 + this.depth) * HEIGHT + vertOffset);
    }
}
