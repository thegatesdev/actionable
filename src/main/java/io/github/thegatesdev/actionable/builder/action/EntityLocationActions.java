package io.github.thegatesdev.actionable.builder.action;

import io.github.thegatesdev.actionable.builder.ActionBuilder;
import io.github.thegatesdev.actionable.registry.BuilderRegistry;
import io.github.thegatesdev.maple.read.ReadableOptions;
import io.github.thegatesdev.threshold.util.twin.Twin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

import static io.github.thegatesdev.actionable.Actionable.VECTOR;
import static io.github.thegatesdev.actionable.registry.Registries.*;

public final class EntityLocationActions extends BuilderRegistry.Static<Consumer<Twin<Entity, Location>>, ActionBuilder<Twin<Entity, Location>>> {
    public EntityLocationActions(String id) {
        super(id);
        info().description("An action executed on an actor entity and a target location.");
    }

    @Override
    public void registerStatic() {
        register(ActionBuilder.moreFactory(ENTITY_LOCATION_ACTION));
        register(ActionBuilder.ifElseFactory(ENTITY_LOCATION_CONDITION, ENTITY_LOCATION_ACTION));
        register(ActionBuilder.splitFactory(ENTITY_ACTION, LOCATION_ACTION));
        register(ActionBuilder.loopFactory(ENTITY_LOCATION_ACTION));
        register(ActionBuilder.loopWhileFactory(ENTITY_LOCATION_ACTION, ENTITY_LOCATION_CONDITION));

        register(new ActionBuilder<>("teleport_to", (data, twin) -> twin.actor().teleport(twin.target().setDirection(twin.actor().getLocation().getDirection()).add(data.getObject("offset", Vector.class))), new ReadableOptions().add("offset", VECTOR, new Vector(0, 0, 0))));

        register(new ActionBuilder<>("look_at", (data, twin) -> {
            final Location location = twin.target().clone();
            // Let Location do the direction calculations for us.
            location.setDirection(location.subtract(twin.actor().getLocation()).toVector());
            twin.actor().setRotation(location.getYaw(), location.getPitch());
        }));

        register(new ActionBuilder<>("run_in_world", (data, twin) -> data.<Consumer<World>>getUnsafe("action").accept(twin.target().getWorld()), new ReadableOptions()
            .add("action", WORLD_ACTION)
        ));

        register(new ActionBuilder<>("run_at_target", (data, twin) -> data.<Consumer<Location>>getUnsafe("action").accept(twin.target()), new ReadableOptions()
            .add("action", LOCATION_ACTION)
        ));
    }
}
