package io.github.thegatesdev.actionable.builder.action;

import io.github.thegatesdev.actionable.builder.ActionBuilder;
import io.github.thegatesdev.actionable.registry.BuilderRegistry;
import io.github.thegatesdev.maple.read.Options;
import io.github.thegatesdev.threshold.world.WorldModification;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

import static io.github.thegatesdev.actionable.Actionable.VECTOR;
import static io.github.thegatesdev.actionable.registry.Registries.*;
import static io.github.thegatesdev.maple.read.Readable.*;

public final class LocationActions extends BuilderRegistry.Static<Consumer<Location>, ActionBuilder<Location>> {
    public LocationActions(String id) {
        super(id);
    }

    @Override
    public void registerStatic() {
        register(ActionBuilder.moreFactory(LOCATION_ACTION));
        register(ActionBuilder.ifElseFactory(LOCATION_CONDITION, LOCATION_ACTION));
        register(ActionBuilder.loopFactory(LOCATION_ACTION));
        register(ActionBuilder.loopWhileFactory(LOCATION_ACTION, LOCATION_CONDITION));

        register(new ActionBuilder<>("move", (data, location) -> {
            Vector dir = data.getUnsafe("direction");
            if (data.getBoolean("relative")) dir = location.getDirection().multiply(dir);
            location.add(dir);
        }, new Options()
            .add("direction", VECTOR)
            .add("relative", bool(), false)
        ));

        register(new ActionBuilder<>("run_in_world", (data, location) -> data.<Consumer<World>>getUnsafe("action").accept(location.getWorld()), new Options()
            .add("action", WORLD_ACTION)
        ));

        register(new ActionBuilder<>("play_sound", (data, location) -> {
            final World world = location.getWorld();
            if (world == null) return;
            final Sound sound = data.getUnsafe("sound");
            int pitch = data.getInt("pitch");
            float volume = data.getFloat("volume");
            world.playSound(location, sound, SoundCategory.AMBIENT, volume, pitch);
        }, new Options()
            .add("sound", enumeration(Sound.class))
            .add("pitch", number(), 0)
            .add("volume", number(), 1f)
        ));

        register(new ActionBuilder<>("fill", (data, location) -> {
            final World world = location.getWorld();
            if (world == null) return;
            final Material material = data.getUnsafe("block");
            final Vector loc = location.toVector();
            final Vector from = data.<Vector>getUnsafe("from").clone().add(loc);
            final Vector to = data.<Vector>getUnsafe("to").clone().add(loc);
            final var mod = WorldModification.sync(world);
            mod.fill(from.getBlockX(), from.getBlockY(), from.getBlockZ(), to.getBlockX(), to.getBlockY(), to.getBlockZ(), material);
            mod.update();
        }, new Options()
            .add("from", VECTOR)
            .add("to", VECTOR)
            .add("block", enumeration(Material.class))
        ));

        register(new ActionBuilder<>("set_block", (data, location) -> {
            final World world = location.getWorld();
            if (world == null) return;
            world.getBlockAt(location).setType(data.getUnsafe("block"));
        }, new Options().add("block", enumeration(Material.class))));

        register(new ActionBuilder<>("particle", (data, location) -> {
            location.checkFinite();
            final World world = location.getWorld();
            if (world == null) return;
            final Particle particle = data.getObject("particle", Particle.class);
            Object particleData = null;
            if (particle.getDataType() != Void.class) {
                if (particle.getDataType() == BlockData.class) {
                    Material material = data.getUnsafe("material");
                    if (material == null) return;
                    particleData = material.createBlockData(); // TODO Create block data beforehand
                }
            } else particleData = data.getDouble("speed");
            final Vector vector = data.getObject("vector", Vector.class);
            world.spawnParticle(particle, location.add(data.getObject("offset", Vector.class)), data.getInt("amount"), vector.getX(), vector.getY(), vector.getZ(), particleData);
        }, new Options()
            .add("particle", enumeration(Particle.class))
            .optional("material", enumeration(Material.class))
            .add("amount", number(), 1)
            .add("speed", number(), 1d)
            .add("offset", VECTOR, new Vector())
            .add("vector", VECTOR, new Vector())
        ));

        register(new ActionBuilder<>("summon", (data, location) -> {
            location.checkFinite();
            final World world = location.getWorld();
            if (world == null) return;
            EntityType entityType = data.getUnsafe("entity_type");
            final Entity spawnedEntity = world.spawnEntity(location, entityType);
            final Consumer<Entity> mobAction = data.getUnsafe("action", null);
            if (mobAction != null) mobAction.accept(spawnedEntity);
        }, new Options()
            .add("entity_type", enumeration(EntityType.class))
            .optional("action", ENTITY_ACTION)
        ));

        register(new ActionBuilder<>("lightning", (data, location) -> {
            final World world = location.getWorld();
            if (world == null) return;
            if (data.getBoolean("damage"))
                world.spigot().strikeLightning(location, data.getBoolean("silent"));
            else world.spigot().strikeLightningEffect(location, data.getBoolean("silent"));
        }, new Options()
            .add("damage", bool(), true)
            .add("silent", bool(), false)
        ));
    }
}
