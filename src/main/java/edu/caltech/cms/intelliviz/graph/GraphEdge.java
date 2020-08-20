package edu.caltech.cms.intelliviz.graph;

import edu.caltech.cms.intelliviz.graph.ui.ClassNode;
import edu.caltech.cms.intelliviz.graph.ui.TextLabel;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;

public class GraphEdge {

    public Node source;
    public Node dest;
    public String declaringType;
    public TextLabel label;
    boolean styleChecked = false;
    double arcOffset = 0;

    //private Line2D line;

    GraphEdge(Node from, Node to, String label) {
        this.source = from;
        this.dest = to;
        this.label = new TextLabel(label);
    }

    public GraphEdge(Node from, Node to, String label, String declaringType) {
        this(from, to, label);
        this.declaringType = declaringType;
    }

    public void draw(Graphics2D g) {
        if (this.source.isHidden() || this.dest.isHidden()) {
            return;
        }

        try {
            Point2D origin = source.getOrigin(this);
            Point2D destPt = dest.getTarget(origin.getX(), origin.getY());


            GraphEdge otherEdge = null;
            if (!styleChecked) {
                for (GraphEdge other : this.dest.getChildren()) {
                    int count = 1;
                    if (!other.styleChecked && other.dest.equals(this.source)) {
                        other.styleChecked = true;
                        this.styleChecked = true;
                        this.arcOffset = 1 + count * 0.3;
                        other.arcOffset = -1 - count * 0.3;
                        otherEdge = other;
                    }
                }
            }

            g.setColor(Color.black);
            if (arcOffset == 0) {
                g.drawLine((int) origin.getX(), (int) origin.getY(), (int) destPt.getX(), (int) destPt.getY());
                if (this.source instanceof ClassNode) {
                    Point2D originProj = getCenterTargetingProjection(this.source, destPt.getX(), destPt.getY());
                    label.draw(g, (originProj.getX() + destPt.getX()) / 2, (originProj.getY() + destPt.getY()) / 2);

                } else {
                    label.draw(g, (origin.getX() + destPt.getX()) / 2, (origin.getY() + destPt.getY()) / 2);
                }
            } else {
                Point2D newOrigin = GraphEdge.getCenterTargetingProjection(this.source, destPt.getX(), destPt.getY());
                drawArc(g, newOrigin, destPt, arcOffset);
                drawArc(g, destPt, newOrigin, arcOffset);
            }



            // Render Arrow Head
            Graphics2D g2 = (Graphics2D) g.create();
            double angle = Math.atan2(destPt.getY() - origin.getY(), destPt.getX() - origin.getX()) - Math.PI / 2d;
            Polygon rotatedArrow = new Polygon();

            // Rudimentary rotation matrix because AffineTransform breaks on my computer
            rotatedArrow.addPoint(0, 0);
            rotatedArrow.addPoint((int) (-4 * Math.cos(angle) + 8 * Math.sin(angle)),
                    (int) (-4 * Math.sin(angle) - 8 * Math.cos(angle)));
            rotatedArrow.addPoint((int) (4 * Math.cos(angle) + 8 * Math.sin(angle)),
                    (int) (4 * Math.sin(angle) - 8 * Math.cos(angle)));
            rotatedArrow.translate((int) destPt.getX(), (int) destPt.getY());
            g2.fill(rotatedArrow);
            g2.dispose();
        } catch (NullPointerException e) {
            System.out.println();
        }
    }

    public static Point2D getCenterTargetingProjection(Node dest, double originX, double originY) {
        double  x2_c = dest.getX() + dest.getWidth() / 2,
                y2_c = dest.getY() + dest.getHeight() / 2,
                x2 = 0, y2 = 0;

        double dx = x2_c - originX;
        double dy = y2_c - originY;
        double m = dy/dx;

        if (Math.abs(m) > Math.abs(dest.getHeight() / dest.getWidth())) {
            if (dy < 0) {
                x2 = x2_c + (dest.getHeight()/2) / m;
                y2 = y2_c + dest.getHeight()/2;
            } else {
                x2 = x2_c - (dest.getHeight()/2) / m;
                y2 = y2_c - dest.getHeight()/2;
            }
        } else {
            if (dx < 0) {
                x2 = x2_c + dest.getWidth()/2;
                y2 = y2_c + dest.getWidth()/2 * m;
            } else {
                x2 = x2_c - dest.getWidth()/2;
                y2 = y2_c - dest.getWidth()/2 * m;
            }
        }
        
        return new Point2D.Double(x2, y2); 
        
    }

    private void drawArc(Graphics2D g2d, Point2D newOrigin, Point2D destPt, double arcOffset) {
        double dy = destPt.getY() - newOrigin.getY();
        double dx = destPt.getX() - newOrigin.getX();
        double dist = Math.sqrt(dx * dx + dy * dy);
        double circleRadius = arcOffset * dist;

        double normFactor = Math.sqrt(circleRadius * circleRadius - 0.25 * dist * dist) / Math.abs(dist);

        // calculate midpoint of line, then offset along perpendicular bisector to find center of circle
        double centerX = 0.5 * (newOrigin.getX() + destPt.getX()) + normFactor * dy;
        double centerY = 0.5 * (newOrigin.getY() + destPt.getY()) - normFactor * dx;

        // upper-left corner of circumscribing square can be found by subtracting radius
        double startAngle = (Math.atan2(centerY - newOrigin.getY(), newOrigin.getX() - centerX) * 180 / Math.PI);
        double stopAngle = (Math.atan2(centerY - destPt.getY(), destPt.getX() - centerX) * 180 / Math.PI);

        int startAngle2 = (int)Math.floor(Math.min(startAngle, stopAngle));
        int stopAngle2 = (int)Math.ceil(Math.max(startAngle, stopAngle));

        int sweep = stopAngle2 - startAngle2;
        if (Math.abs(sweep) > 180) {
            if (sweep < 0) {
                sweep = -360 + sweep;
            } else {
                sweep = 360 - sweep;
            }
        }

        g2d.drawArc((int)(centerX - circleRadius), (int)(centerY - circleRadius), 2 * (int)circleRadius, 2 * (int)circleRadius,
                startAngle2, sweep);
    }

}
