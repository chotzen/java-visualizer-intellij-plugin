package edu.caltech.cms.intelliviz.graph;

import java.awt.geom.Point2D;

public interface Targetable {
    Point2D getTarget(double originX, double originY);
}
