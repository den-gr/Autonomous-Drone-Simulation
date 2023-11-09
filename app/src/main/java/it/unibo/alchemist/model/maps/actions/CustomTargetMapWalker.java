package it.unibo.alchemist.model.maps.actions;


import it.unibo.alchemist.model.GeoPosition;
import it.unibo.alchemist.model.maps.movestrategies.routing.IgnoreStreets;
import it.unibo.alchemist.model.maps.movestrategies.target.FollowTargetOnMap;
import it.unibo.alchemist.model.movestrategies.speed.ConstantSpeed;
import it.unibo.alchemist.model.maps.MapEnvironment;
import it.unibo.alchemist.model.Molecule;
import it.unibo.alchemist.model.Node;
import it.unibo.alchemist.model.Reaction;
import it.unibo.alchemist.model.RoutingService;
import it.unibo.alchemist.model.RoutingServiceOptions;

import javax.annotation.Nonnull;


public class CustomTargetMapWalker<T, O extends RoutingServiceOptions<O>, S extends RoutingService<GeoPosition, O>>
        extends MoveOnMap<T, O, S> {

    /**
     * Default speed in meters per second.
     */
    public static final double DEFAULT_SPEED = 1.5;


    private static final long serialVersionUID = 5097382908560832035L;

    /**
     * @param environment         the environment
     * @param node                the node
     * @param reaction            the reaction. Will be used to compute the distance to walk in
     *                            every step, relying on {@link Reaction}'s getRate() method.
     * @param trackMolecule       the molecule to track. Its value will be read when it is time
     *                            to compute a new target. If it is a {@link GeoPosition},
     *                            it will be used as-is. If it is an {@link Iterable}, the first
     *                            two values (if they are present and they are numbers, or
     *                            Strings parse-able to numbers) will be used to create a new
     *                            {@link GeoPosition}. Otherwise, the {@link Object} bound
     *                            to this {@link Molecule} will be converted to a String, and
     *                            the String will be parsed using the float regular expression
     *                            matcher in Javalib.
     */
    public CustomTargetMapWalker(
            @Nonnull final MapEnvironment<T, O, S> environment,
            @Nonnull final Node<T> node,
            @Nonnull final Reaction<T> reaction,
            @Nonnull final Molecule trackMolecule
    ) {
        super(
                environment,
                node,
                new IgnoreStreets<>(),
                new ConstantSpeed<>(reaction, DEFAULT_SPEED),
                new FollowTargetOnMap<>(environment, node, trackMolecule)
        );
    }
}