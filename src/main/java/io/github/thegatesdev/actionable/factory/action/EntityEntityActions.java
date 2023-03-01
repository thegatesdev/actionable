package io.github.thegatesdev.actionable.factory.action;

import io.github.thegatesdev.actionable.factory.ActionFactory;


import io.github.thegatesdev.actionable.Factories;
import io.github.thegatesdev.actionable.util.twin.Twin;
import io.github.thegatesdev.mapletree.data.Readable;
import io.github.thegatesdev.mapletree.data.ReadableOptions;

import io.github.thegatesdev.mapletree.registry.FactoryRegistry;
import io.github.thegatesdev.mapletree.registry.Identifiable;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

public final class EntityEntityActions extends FactoryRegistry<Consumer<Twin<Entity, Entity>>, ActionFactory<Twin<Entity, Entity>>> {
    public EntityEntityActions(String id) {
        super(id, Identifiable::id);
    }

    @Override
    public void registerStatic() {
        register(ActionFactory.multipleFactory(Factories.ENTITY_ENTITY_ACTION));
        register(ActionFactory.ifElseFactory(Factories.ENTITY_ENTITY_CONDITION, Factories.ENTITY_ENTITY_ACTION));
        register(ActionFactory.flippedFactory(Factories.ENTITY_ENTITY_ACTION));
        register(ActionFactory.splitFactory(Factories.ENTITY_ACTION, Factories.ENTITY_ACTION));
        register(ActionFactory.loopFactory(Factories.ENTITY_ENTITY_ACTION));
        register(ActionFactory.loopWhileFactory(Factories.ENTITY_ENTITY_ACTION, Factories.ENTITY_ENTITY_CONDITION));

        register(new ActionFactory<>("launch_target", (data, twin) -> {
            final double amount = data.getDouble("amount");
            final double y = data.getDouble("up");

            final Location actorLoc = twin.actor().getLocation();
            final Entity launched = twin.target();
            final Vector direction = launched.getLocation().subtract(actorLoc).multiply(amount).add(0, y, 0).toVector();

            if (data.getBoolean("set")) launched.setVelocity(direction);
            else launched.setVelocity(launched.getVelocity().add(direction));
        }, new ReadableOptions()
                .add("amount", Readable.primitive(Number.class), 1)
                .add("up", Readable.primitive(Number.class), 0)
                .add("set", Readable.primitive(Boolean.class), false)
        ));

        register(new ActionFactory<>("run_entity_location_action", (data, twin) -> data.<Consumer<Twin<Entity, Location>>>getUnsafe("action").accept(Twin.of(twin.actor(), twin.target().getLocation())), new ReadableOptions().add("action", Factories.ENTITY_LOCATION_ACTION)));

        register(new ActionFactory<>("attack_target", (data, twin) -> {
            if (twin.actor() instanceof LivingEntity lv) lv.attack(twin.target());
        }));

        register(new ActionFactory<>("damage_target", (data, twin) -> {
            if (twin.target() instanceof LivingEntity livingTarget)
                livingTarget.damage(data.getDouble("amount"), twin.actor());
        }, new ReadableOptions().add("amount", Readable.primitive(Number.class))));

        register(new ActionFactory<>("mount_target", (data, twin) -> twin.target().addPassenger(twin.actor())));
    }
}
