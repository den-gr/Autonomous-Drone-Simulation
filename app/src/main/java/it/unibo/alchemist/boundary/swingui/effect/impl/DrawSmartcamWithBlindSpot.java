package it.unibo.alchemist.boundary.swingui.effect.impl;

import it.unibo.alchemist.boundary.swingui.effect.api.Effect;
import it.unibo.alchemist.boundary.ui.api.Wormhole2D;
import it.unibo.alchemist.model.actions.CameraSeeWithBlindSpot;
import it.unibo.alchemist.model.geometry.AwtShapeCompatible;
import it.unibo.alchemist.model.geometry.Shape;
import it.unibo.alchemist.model.maps.environments.OSMEnvironment;
import it.unibo.alchemist.model.molecules.SimpleMolecule;
import it.unibo.alchemist.model.positions.Euclidean2DPosition;
import it.unibo.alchemist.model.Environment;
import it.unibo.alchemist.model.Node;
import it.unibo.alchemist.model.Position2D;
import it.unibo.alchemist.model.physics.properties.OccupiesSpaceProperty;
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment;
import org.jooq.lambda.function.Consumer2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.io.Serial;

@SuppressWarnings("deprecation")
public final class DrawSmartcamWithBlindSpot implements Effect {
    private static final Logger LOGGER = LoggerFactory.getLogger(DrawSmartcamWithBlindSpot.class);
    private static final SimpleMolecule WANTED = new SimpleMolecule("wanted");
    @Serial
    private static final long serialVersionUID = 1L;
    private boolean alreadyLogged;
    private static final Color transparentBlack = new Color(0.0f, 0.0f, 0.0f, 0.1f);

    @Override
    public <T, P extends Position2D<P>> void apply(
            final Graphics2D graphics,
            final Node<T> node,
            final Environment<T, P> environment,
            final Wormhole2D<P> wormhole
    ) {
        final double zoom = wormhole.getZoom();
        final Point viewPoint = wormhole.getViewPoint(environment.getPosition(node));
        final int x = viewPoint.x;
        final int y = viewPoint.y;
        if (environment instanceof final Physics2DEnvironment<?> physicsEnv) {
            @SuppressWarnings("unchecked")
            final Physics2DEnvironment<T> physicsEnvironment = (Physics2DEnvironment<T>) physicsEnv;
            drawShape(graphics, node, physicsEnvironment, zoom, x, y);
            drawFieldOfView(graphics, node, physicsEnvironment, zoom, x, y);
        } else {
            logOnce("DrawSmartcamWithBlindSpot only works with EuclideanPhysics2DEnvironment", Logger::warn);
        }
    }

    @Override
    public Color getColorSummary() {
        return Color.GREEN;
    }

    private <T> void drawShape(
            final Graphics2D graphics,
            final Node<T> node,
            final Physics2DEnvironment<T> environment,
            final double zoom,
            final int x,
            final int y
    ) {
        @SuppressWarnings("unchecked")
        final Shape<?, ?> geometricShape = node.asPropertyOrNull(OccupiesSpaceProperty.class) != null
                ? node.asProperty(OccupiesSpaceProperty.class).getShape()
                : null;
        if (geometricShape instanceof AwtShapeCompatible) {
            final AffineTransform transform = getTransform(x, y, zoom, getRotation(node, environment));
            final java.awt.Shape shape = transform.createTransformedShape(((AwtShapeCompatible) geometricShape).asAwtShape());
            if (node.contains(WANTED)) {
                graphics.setColor(Color.RED);
            } else {
                graphics.setColor(Color.GREEN);
            }
            graphics.draw(shape);
        } else {
            logOnce("DrawSmartcamWithBlindSpot only works with shapes implementing AwtShapeCompatible", Logger::warn);
        }
    }

    private <T> void drawFieldOfView(
            final Graphics2D graphics,
            final Node<T> node,
            final Physics2DEnvironment<T> environment,
            final double zoom,
            final int x,
            final int y
    ) {
        final AffineTransform transform = getTransform(x, y, zoom, getRotation(node, environment));
        graphics.setColor(Color.BLUE);
        node.getReactions()
                .stream()
                .flatMap(r -> r.getActions().stream())
                .filter(a -> a instanceof CameraSeeWithBlindSpot)
                .map(a -> (CameraSeeWithBlindSpot) a)
                .forEach(a -> {
                    final double angle = a.getAngle();
                    final double startAngle = -angle / 2;
                    final double d = a.getDistance();
                    final java.awt.Shape fov = new Arc2D.Double(-d, -d, d * 2, d * 2, startAngle, angle, Arc2D.PIE);
                    final double sd = a.getBlindSpotDistance();
                    final java.awt.Shape blindSpotPie = new Arc2D.Double(-sd, -sd, sd * 2, sd * 2, startAngle, angle, Arc2D.PIE);
                    final java.awt.Shape blindSpotOpen = new Arc2D.Double(-sd, -sd, sd * 2, sd * 2, startAngle + 0.1, angle + 0.1 , Arc2D.OPEN);
                    graphics.setColor(transparentBlack);
                    graphics.fill(transform.createTransformedShape(blindSpotPie));
                    graphics.setColor(Color.BLUE);
                    graphics.draw(transform.createTransformedShape(fov));
                    graphics.draw(transform.createTransformedShape(blindSpotOpen));
                });
    }

    private <T> double getRotation(final Node<T> node, final Physics2DEnvironment<T> environment) {
        final Euclidean2DPosition direction = environment.getHeading(node);
        return Math.atan2(direction.getY(), direction.getX());
    }

    private AffineTransform getTransform(final int x, final int y, final double zoom, final double rotation) {
        final AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        transform.scale(zoom, zoom);
        transform.rotate(-rotation); // invert angle because the y-axis is inverted in the gui
        return transform;
    }

    private void logOnce(final String message, final Consumer2<Logger, String> logger) {
        if (!alreadyLogged) {
            logger.accept(LOGGER, message);
            alreadyLogged = true;
        }
    }
}