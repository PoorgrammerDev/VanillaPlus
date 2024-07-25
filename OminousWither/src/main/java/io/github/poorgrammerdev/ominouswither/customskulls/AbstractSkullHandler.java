package io.github.poorgrammerdev.ominouswither.customskulls;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import io.github.poorgrammerdev.ominouswither.OminousWither;
import io.github.poorgrammerdev.ominouswither.ParticleInfo;
import io.github.poorgrammerdev.ominouswither.backend.CoroutineManager;
import io.github.poorgrammerdev.ominouswither.coroutines.PersistentTrackingParticle;

/**
 * Abstract class for custom dangerous (blue) skulls
 * @author Thomas Tran
 */
public abstract class AbstractSkullHandler {
    protected static final int SKULL_LIFESPAN = 500;

    protected final double initialSpeed;
    protected final ParticleInfo trackingParticle;
    protected final OminousWither plugin;

    public AbstractSkullHandler(final OminousWither plugin, final double initialSpeed, final ParticleInfo trackingParticle) {
        this.plugin = plugin;
        this.initialSpeed = initialSpeed;
        this.trackingParticle = trackingParticle;
    }

    /**
     * Called on the firing of every plugin-generated blue skull
     * Does not include naturally occurring blue skulls
     * This method does not have to handle tagging the skull with PDC
     * 
     * Base method sets velocity, lifespan, and tracking particle
     * @param skull fired projectile
     */
    public void onSpawn(final WitherSkull skull) {
        //Velocity
        skull.setVelocity(skull.getVelocity().multiply(this.initialSpeed));

        //Tracking particle
        CoroutineManager.getInstance().enqueue(new PersistentTrackingParticle(
            this.plugin,
            (uuid) -> {
                //Entity must still exist
                final Entity entity = plugin.getServer().getEntity(uuid);
                if (entity == null || entity.isDead()) return true;

                //Remove the skull after its lifespan has finished
                //TODO: is this a bad place to do it? it is technically inside of a particle check
                if (entity.getTicksLived() >= SKULL_LIFESPAN) {
                    entity.remove();
                    return true;
                }

                //Continue playing particle as normal
                return false;
            },
            skull.getUniqueId(),
            new Vector(0, 0.25, 0),
            this.trackingParticle
        ));
    }

    /**
     * Method is passed when a skull tagged with this skull's tag hits something
     * This is intentionally not to be marked with @EventHandler,
     * a separate handler will pass the event object to this method
     * @param event passed event
     * @param wither the wither that shot the skull
     */
    public abstract void onHit(final ProjectileHitEvent event, final Wither wither);

    /**
     * @return Unique tag for this skull, will be saved to PDC
     */
    public abstract String getSkullTag();
    
}
