package io.github.thegatesdev.actionable.builder.action;

import io.github.thegatesdev.actionable.builder.ActionBuilder;
import io.github.thegatesdev.actionable.registry.BuilderRegistry;
import io.github.thegatesdev.maple.read.Options;
import io.github.thegatesdev.threshold.util.twin.Twin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

import static io.github.thegatesdev.actionable.Actionable.VECTOR;
import static io.github.thegatesdev.actionable.registry.Registries.*;
import static io.github.thegatesdev.maple.read.Readable.bool;
import static io.github.thegatesdev.maple.read.Readable.number;

public final class EntityEntityActions extends BuilderRegistry.Static<Consumer<Twin<Entity, Entity>>, ActionBuilder<Twin<Entity, Entity>>> {
    public EntityEntityActions(String id) {
        super(id);
    }

    @Override
    public void registerStatic() {
        register(ActionBuilder.moreFactory(ENTITY_ENTITY_ACTION));
        register(ActionBuilder.ifElseFactory(ENTITY_ENTITY_CONDITION, ENTITY_ENTITY_ACTION));
        register(ActionBuilder.flippedFactory(ENTITY_ENTITY_ACTION));
        register(ActionBuilder.splitFactory(ENTITY_ACTION, ENTITY_ACTION));
        register(ActionBuilder.loopFactory(ENTITY_ENTITY_ACTION));
        register(ActionBuilder.loopWhileFactory(ENTITY_ENTITY_ACTION, ENTITY_ENTITY_CONDITION));

        register(new ActionBuilder<>("launch_target", (data, twin) -> {
            final Entity launched = twin.target();
            final Vector direction = launched.getLocation().toVector().subtract(twin.actor().getLocation().toVector())
                .normalize().multiply(data.getUnsafe("multiplier")).add(data.getUnsafe("addition"));

            if (data.getBoolean("set")) launched.setVelocity(direction);
            else launched.setVelocity(launched.getVelocity().add(direction));
        }, new Options()
            .add("multiplier", VECTOR, new Vector(1, 1, 1))
            .add("addition", VECTOR, new Vector())
            .add("set", bool(), false)
        ));

        register(new ActionBuilder<>("run_at_target", (data, twin) -> data.<Consumer<Twin<Entity, Location>>>getUnsafe("action").accept(Twin.of(twin.actor(), twin.target().getLocation())), new Options().add("action", ENTITY_LOCATION_ACTION)));

        register(new ActionBuilder<>("attack_target", (data, twin) -> {
            if (twin.actor() instanceof LivingEntity lv) lv.attack(twin.target());
        }));

        register(new ActionBuilder<>("damage_target", (data, twin) -> {
            if (twin.target() instanceof LivingEntity livingTarget)
                livingTarget.damage(data.getDouble("amount"), twin.actor());
        }, new Options().add("amount", number())));

        register(new ActionBuilder<>("mount_target", (data, twin) -> twin.target().addPassenger(twin.actor())));
    }
}
