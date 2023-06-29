package io.github.thegatesdev.actionable.factory.action;

import io.github.thegatesdev.actionable.factory.ActionFactory;
import io.github.thegatesdev.maple.read.ReadableOptions;
import io.github.thegatesdev.maple.registry.StaticFactoryRegistry;
import io.github.thegatesdev.maple.registry.struct.Identifiable;
import io.github.thegatesdev.threshold.world.WorldModification;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

import static io.github.thegatesdev.actionable.Actionable.VECTOR;
import static io.github.thegatesdev.actionable.Factories.*;
import static io.github.thegatesdev.maple.read.Readable.*;

public final class LocationActions extends StaticFactoryRegistry<Consumer<Location>, ActionFactory<Location>> {
    public LocationActions(String id) {
        super(id, Identifiable::id);
        info().description("An action executed at a location in a world.");
    }

    @Override
    public void registerStatic() {
        register(ActionFactory.moreFactory(LOCATION_ACTION));
        register(ActionFactory.ifElseFactory(LOCATION_CONDITION, LOCATION_ACTION));
        register(ActionFactory.loopFactory(LOCATION_ACTION));
        register(ActionFactory.loopWhileFactory(LOCATION_ACTION, LOCATION_CONDITION));

        register(new ActionFactory<>("move", (data, location) -> {
            Vector dir = data.getUnsafe("direction");
            if (data.getBoolean("relative")) dir = location.getDirection().multiply(dir);
            location.add(dir);
        }, new ReadableOptions()
                .add("direction", VECTOR)
                .add("relative", bool(), false)
        ));

        register(new ActionFactory<>("run_in_world", (data, location) -> data.<Consumer<World>>getUnsafe("action").accept(location.getWorld()), new ReadableOptions()
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
                .add("sound", enumeration(Sound.class))
                .add("pitch", number(), 0)
                .add("volume", number(), 1f)
        ));

        register(new ActionFactory<>("fill", (data, location) -> {
            final World world = location.getWorld();
            if (world == null) return;
            final Material material = data.getUnsafe("block");
            final Vector loc = location.toVector();
            final Vector from = data.<Vector>getUnsafe("from").clone().add(loc);
            final Vector to = data.<Vector>getUnsafe("to").clone().add(loc);
            final var mod = WorldModification.sync(world);
            mod.fill(from.getBlockX(), from.getBlockY(), from.getBlockZ(), to.getBlockX(), to.getBlockY(), to.getBlockZ(), material);
            mod.update();
        }, new ReadableOptions()
                .add("from", VECTOR)
                .add("to", VECTOR)
                .add("block", enumeration(Material.class))
        ));

        register(new ActionFactory<>("set_block", (data, location) -> {
            final World world = location.getWorld();
            if (world == null) return;
            world.getBlockAt(location).setType(data.getUnsafe("block"));
        }, new ReadableOptions().add("block", enumeration(Material.class))));

        register(new ActionFactory<>("particle", (data, location) -> {
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
        }, new ReadableOptions()
                .add("particle", enumeration(Particle.class))
                .addOptional("material", enumeration(Material.class))
                .add("amount", number(), 1)
                .add("speed", number(), 1d)
                .add("offset", VECTOR, new Vector())
                .add("vector", VECTOR, new Vector())
        ));

        register(new ActionFactory<>("summon", (data, location) -> {
            location.checkFinite();
            final World world = location.getWorld();
            if (world == null) return;
            EntityType entityType = data.getUnsafe("entity_type");
            final Entity spawnedEntity = world.spawnEntity(location, entityType);
            final Consumer<Entity> mobAction = data.getUnsafe("action", null);
            if (mobAction != null) mobAction.accept(spawnedEntity);
        }, new ReadableOptions()
                .add("entity_type", enumeration(EntityType.class))
                .addOptional("action", ENTITY_ACTION)
        ));

        register(new ActionFactory<>("lightning", (data, location) -> {
            final World world = location.getWorld();
            if (world == null) return;
            if (data.getBoolean("damage"))
                world.spigot().strikeLightning(location, data.getBoolean("silent"));
            else world.spigot().strikeLightningEffect(location, data.getBoolean("silent"));
        }, new ReadableOptions()
                .add("damage", bool(), true)
                .add("silent", bool(), false)
        ));
    }
}
