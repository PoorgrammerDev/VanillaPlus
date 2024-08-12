package io.github.poorgrammerdev.ominouswither.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * Utility class to draw particle shapes
 * @author Thomas Tran
 */
public final class ParticleShapes {

    /**
     * Displays a particle circle on the Y axis at the given location
     * @param particle information of particle to display
     * @param radius radius of circle
     * @param density how thick to make the line
     * @param location where to display it
     */
    public static void circle(final ParticleInfo particle, final double radius, final double density, Location location) {
        final World world = location.getWorld();
        if (world == null) return;

        //Even though this method shouldn't have any effect on the location object,
        //make a clone just to be extra safe
        location = location.clone();

        //Iterates through angles in radians up to 2pi
        for (double theta = 0; theta <= 2*Math.PI; theta += Math.PI / density) {

            //Calculates offsets from the center 
            double x = radius * Math.cos(theta);
            double z = radius * Math.sin(theta);

            //Add offset, spawn particle, and subtract offset to reset for the next iter
            location.add(x, 0, z);
            particle.spawnParticle(world, location);
            location.subtract(x, 0, z);
        }
    }

    /**
     * Displays a partial particle circle on the Y axis at the given location
     * @param particle information of particle to display
     * @param radius radius of circle
     * @param density how thick to make the line
     * @param location where to display it
     * @param limit what angle to draw the circle up until, in interval [0, 2pi]
     */
    public static void partialCircle(final ParticleInfo particle, final double radius, final double density, Location location, final double limit) {
        final World world = location.getWorld();
        if (world == null) return;

        //Even though this method shouldn't have any effect on the location object,
        //make a clone just to be extra safe
        location = location.clone();

        //Iterates through angles in radians up to 2pi
        for (double theta = 0; theta <= limit; theta += Math.PI / density) {

            //Calculates offsets from the center 
            double x = radius * Math.cos(theta);
            double z = radius * Math.sin(theta);

            //Add offset, spawn particle, and subtract offset to reset for the next iter
            location.add(x, 0, z);
            particle.spawnParticle(world, location);
            location.subtract(x, 0, z);
        }
    }

    /**
     * Displays a particle line from the origin to the target
     * @param particle information of particle to display
     * @param origin one end of the line
     * @param target the other end of the line (order does not matter, you can swap these two)
     * @param density how thick to make the line
     */
    public static void line(final ParticleInfo particle, final Location origin, final Location target, final double density) {
        final World world = origin.getWorld();
        final World targetWorld = target.getWorld();
        if (world == null || targetWorld == null || !world.equals(targetWorld)) return;

        final Vector begin = origin.toVector();
        final Vector end = target.toVector();
        for (double t = 0.0; t <= 1.0D; t += (1.0D / density)) {
            particle.spawnParticle(
                world,
                Utils.lerp(begin, end, t).toLocation(world, origin.getYaw(), origin.getPitch())
            );
        }
    }
 
}
