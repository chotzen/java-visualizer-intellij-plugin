package edu.caltech.cms.intelliviz.graph;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

public class GraphEdge {
    public INode source;
    public INode dest;
    public String declaringType;
    public TextLabel label;

    private Line2D line;


    private GraphEdge(INode from, INode to, String label) {
        this.source = from;
        this.dest = to;
        this.label = new TextLabel(label);
        line = new Line2D.Double();
    }

    public GraphEdge(INode from, INode to, String label, String declaringType) {
        this(from, to, label);
        this.declaringType = declaringType;
    }

    public void draw(Graphics2D g) {
        Point2D origin = source.getOrigin(this); 
        Point2D destPt = this.dest.getTarget(origin.getX(), origin.getY());
        
        g.setColor(Color.black);
        line.setLine(origin.getX(), origin.getY(), destPt.getX(), destPt.getY());
        g.draw(line);

        // Render label

        if (this.source instanceof ClassNode) {
            Point2D originProj = getCenterTargetingProjection(this.source, destPt.getX(), destPt.getY());
            label.draw(g, (originProj.getX() + destPt.getX()) / 2, (originProj.getY() + destPt.getY()) / 2);

        } else {
            label.draw(g, (origin.getX() + destPt.getX()) / 2, (origin.getY() + destPt.getY()) / 2);
        }


        // Render Arrow Head
        Graphics2D g2 = (Graphics2D) g.create();
        double angle = Math.atan2(destPt.getY() - origin.getY(), destPt.getX() - origin.getX()) - Math.PI / 2d;
        Polygon rotatedArrow = new Polygon();

        // Rudimentary rotation matrix because AffineTransform breaks on my computer
        rotatedArrow.addPoint(0, 0);
        rotatedArrow.addPoint(  (int)(-4 * Math.cos(angle) + 8 * Math.sin(angle)),
                                (int)(-4 * Math.sin(angle) - 8 * Math.cos(angle)));
        rotatedArrow.addPoint(  (int)(4 * Math.cos(angle) + 8 * Math.sin(angle)),
                                (int)(4 * Math.sin(angle) - 8 * Math.cos(angle)));
        rotatedArrow.translate((int)destPt.getX(), (int)destPt.getY());
        g2.fill(rotatedArrow);
        g2.dispose();
    }

    public static Point2D getCenterTargetingProjection(INode dest, double originX, double originY) {
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

}
