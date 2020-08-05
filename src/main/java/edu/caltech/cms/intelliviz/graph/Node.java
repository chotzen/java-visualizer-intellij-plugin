package edu.caltech.cms.intelliviz.graph;


import com.intellij.notification.Notification;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.MessageType;
import edu.caltech.cms.intelliviz.graph.ui.NullNode;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public abstract class Node {

    protected int   x = 0,
                    y = 0,
                    width = 0,
                    height = 0;

    protected boolean hidden = false;

    public static final Color HIGHLIGHTED_COLOR = Color.decode("#00FAD8");
    protected static final Color YELLOW = Color.decode("#FAF1C8");
    protected static final Color GREEN = Color.decode("#C8FAD8");
    protected static final Color NULLGREY = Color.decode("#DDDDDD");

    protected static final Font boldFont = new Font("SanSerif", Font.BOLD, 12);
    protected static final Font boldItalic = new Font("SanSerif", Font.BOLD | Font.ITALIC, 12);
    protected static final Font normal = new Font("SanSerif", Font.PLAIN, 12);
    protected static final Font scanner = new Font("Monospaced", Font.PLAIN, 14);

    public abstract void draw(Graphics2D g);

    // Target and origin for edges
    public abstract Point2D getOrigin(GraphEdge edge);
    public abstract Point2D getTarget(double originX, double originY);

    public abstract List<GraphEdge> getChildren();

    public abstract void highlightChanges(Node ref);

    public void drawNode(Graphics2D g) {
        if (!hidden) {
            this.draw(g);
        }
    }

    public boolean contains(double x, double y) {
        return new Rectangle2D.Double(this.x, this.y, this.width, this.height).contains(x, y);
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setPos(double x, double y) {
        this.x = (int)x;
        this.y = (int)y;
    }

    public double getWidth() {
        return this.width;
    }

    public double getHeight() {
        return this.height;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public static void checkReferencesForTypeChange(Node node, Node old) {
        Collector<GraphEdge, ?, Map<String, Node>> graphEdgeMapCollector = Collectors.toMap(edge -> edge.label.toString(), edge -> edge.dest);
        Map<String, Node> oldChildren = old.getChildren().stream().collect(graphEdgeMapCollector);
        Map<String, Node> nodeChildren = node.getChildren().stream().collect(graphEdgeMapCollector);

        for (Map.Entry<String, Node> ent : nodeChildren.entrySet()) {
            if (oldChildren.containsKey(ent.getKey())) {
                if (!(oldChildren.get(ent.getKey()) instanceof NullNode) && ent.getValue() instanceof NullNode) {
                    ent.getValue().highlightChanges(null);
                }
            }
        }
    }

    public static void warnOnClip(int length, int rendered) {
        if (length > rendered) {
            final Notification not = GraphCanvas.VIZ_NOTIFICATIONS
                    .createNotification("Array is too long (length " + length + ") to render completely. " +
                                    "Rendering first " + rendered + " values.",
                            MessageType.WARNING);

            not.notify(ProjectManager.getInstance().getDefaultProject());
        }
    }

    public static <A, B> Map<A, B> clipToLength(Map<A, B> map, int length) {
        return map.entrySet().stream().limit(length)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
