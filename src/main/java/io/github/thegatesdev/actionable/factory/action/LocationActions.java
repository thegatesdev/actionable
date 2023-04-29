package io.github.thegatesdev.actionable.factory.action;

import io.github.thegatesdev.actionable.Actionable;
import io.github.thegatesdev.actionable.factory.ActionFactory;
import io.github.thegatesdev.mapletree.data.Readable;
import io.github.thegatesdev.mapletree.data.ReadableOptions;
import io.github.thegatesdev.mapletree.registry.FactoryRegistry;
import io.github.thegatesdev.mapletree.registry.Identifiable;
import io.github.thegatesdev.threshold.world.WorldModification;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.function.Consumer;

import static io.github.thegatesdev.actionable.Factories.*;

public final class LocationActions extends FactoryRegistry<Consumer<Location>, ActionFactory<Location>> {
    public LocationActions(String id) {
        super(id, Identifiable::id);
    }

    @Override
    public void registerStatic() {
        register(ActionFactory.multipleFactory(LOCATION_ACTION));
        register(ActionFactory.ifElseFactory(LOCATION_CONDITION, LOCATION_ACTION));

        register(new ActionFactory<>("move", (data, location) -> location.add((data.<Vector>getUnsafe("direction"))), new ReadableOptions().add("direction", Actionable.VECTOR)));

        register(new ActionFactory<>("run_world_action", (data, location) -> data.<Consumer<World>>getUnsafe("action").accept(location.getWorld()), new ReadableOptions()
                .add("action", WORLD_ACTION)
        ));

        register(new ActionFactory<>("play_sound", (data, location) -> {
            final World world = location.getWorld();
            if (world == null) return;
            final Sound sound = data.getUnsafe("sound");
            int pitch = data.getInt("pitch");
            float volume = data.getFloat("volume");
            world.playSound(location, sound, SoundCategory.AMBIENT, volume, pitch);
        }, new ReadableOptions()
                .add("sound", Readable.enumeration(Sound.class))
                .add("pitch", Readable.number(), 0)
                .add("volume", Readable.number(), 1f)
        ));

        register(new ActionFactory<>("fill", (data, location) -> {
            final World world = location.getWorld();
            if (world == null) return;
            final Material material = data.getUnsafe("block_type");
            final Vector loc = location.toVector();
            final Vector from = data.<Vector>getUnsafe("from").clone().add(loc);
            final Vector to = data.<Vector>getUnsafe("to").clone().add(loc);
            final var mod = WorldModification.sync(world);
            mod.fill(from.getBlockX(), from.getBlockY(), from.getBlockZ(), to.getBlockX(), to.getBlockY(), to.getBlockZ(), material);
        }, new ReadableOptions()
                .add(List.of("from", "to"), Actionable.VECTOR)
                .add("block_type", Readable.enumeration(Material.class))
        ));

        register(new ActionFactory<>("set_block", (data, location) -> {
            final World world = location.getWorld();
            if (world == null) return;
            world.getBlockAt(location).setType(data.getUnsafe("block_type"));
        }, new ReadableOptions().add("block_type", Readable.enumeration(Material.class))));

        register(new ActionFactory<>("particle", (data, location) -> {
            location.checkFinite();
            final World world = location.getWorld();
            if (world == null) return;
            final Particle particle = data.get("particle", Particle.class);
            Object particleData = null;
            if (particle.getDataType() != Void.class) {
                if (particle.getDataType() == BlockData.class) {
                    particleData = data.get("material", Material.class).createBlockData();
                }
            } else {
                particleData = data.getDouble("speed");
            }
            final Vector vector = data.get("vector", Vector.class);
            world.spawnParticle(particle, location.add(data.get("offset", Vector.class)), data.getInt("amount"), vector.getX(), vector.getY(), vector.getZ(), particleData);
        }, new ReadableOptions()
                .add("particle", Readable.enumeration(Particle.class))
                .add("material", Readable.enumeration(Material.class))
                .add("amount", Readable.number(), 1)
                .add("speed", Readable.number(), 1d)
                .add(List.of("offset", "vector"), Actionable.VECTOR, new Vector(0, 0, 0))
        ));

        register(new ActionFactory<>("summon_mob", (data, location) -> {
            location.checkFinite();
            final World world = location.getWorld();
            if (world == null) return;
            EntityType entityType = data.getUnsafe("entity_type");
            final Entity spawnedEntity = world.spawnEntity(location, entityType);
            final Consumer<Entity> mobAction = data.getUnsafe("entity_action");
            if (mobAction != null) mobAction.accept(spawnedEntity);
        }, new ReadableOptions()
                .add("entity_type", Readable.enumeration(EntityType.class))
                .add("entity_action", ENTITY_ACTION, null)
        ));

        register(new ActionFactory<>("strike_lightning", (data, location) -> {
            final World world = location.getWorld();
            if (world == null) return;
            if (data.getBoolean("effect_only"))
                world.spigot().strikeLightningEffect(location, data.getBoolean("silent"));
            else world.spigot().strikeLightning(location, data.getBoolean("silent"));
        }, new ReadableOptions()
                .add("effect_only", Readable.bool(), false)
                .add("silent", Readable.bool(), false)
        ));
    }
}
