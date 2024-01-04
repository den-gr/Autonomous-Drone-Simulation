package it.unibo.alchemist.model.geometry;

import it.unibo.alchemist.model.positions.Euclidean2DPosition;

import java.awt.geom.Ellipse2D;

public class CustomShapeFactory {
    public static Shape<Euclidean2DPosition, Euclidean2DTransformation> produceEggFormEllipse(double radius, double ratio) {
        return new AwtEuclidean2DShape(new Ellipse2D.Double(-radius * ratio, -radius, radius * 2 * ratio, radius * 2), new Euclidean2DPosition(0, 0));
    }
}
