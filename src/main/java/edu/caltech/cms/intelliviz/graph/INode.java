package edu.caltech.cms.intelliviz.graph;


import com.intellij.notification.Notification;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.MessageType;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public interface INode extends Targetable {

    static final Color HIGHLIGHTED_COLOR = Color.decode("#00FAD8");

    void setPos(double x, double y);
    void draw(Graphics2D g);
    boolean contains(double x, double y);

    double getWidth();
    double getHeight();

    double getX();
    double getY();

    // Target and origin for edges
    Point2D getOrigin(GraphEdge edge);

    ArrayList<GraphEdge> getChildren();

    void highlightChanges(INode ref);

    static void checkReferencesForTypeChange(INode node, INode old) {
        Collector<GraphEdge, ?, Map<String, INode>> graphEdgeMapCollector = Collectors.toMap(edge -> edge.label.toString(), edge -> edge.dest);
        Map<String, INode> oldChildren = old.getChildren().stream().collect(graphEdgeMapCollector);
        Map<String, INode> nodeChildren = node.getChildren().stream().collect(graphEdgeMapCollector);

        for (Map.Entry<String, INode> ent : nodeChildren.entrySet()) {
            if (oldChildren.containsKey(ent.getKey())) {
                if (!(oldChildren.get(ent.getKey()) instanceof NullNode) && ent.getValue() instanceof NullNode) {
                    ent.getValue().highlightChanges(null);
                }
            }
        }
    }

    static void warnOnClip(int length, int rendered) {
        if (length > rendered) {
            final Notification not = GraphCanvas.VIZ_NOTIFICATIONS
                    .createNotification("Array is too long (length " + length + ") to render completely. " +
                                    "Rendering first " + rendered + " values.",
                            MessageType.WARNING);

            not.notify(ProjectManager.getInstance().getDefaultProject());
        }
    }

    static <A, B> Map<A, B> clipToLength(Map<A, B> map, int length) {
        return map.entrySet().stream().limit(length)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
