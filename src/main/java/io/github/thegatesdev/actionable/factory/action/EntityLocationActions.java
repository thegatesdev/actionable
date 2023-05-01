package io.github.thegatesdev.actionable.factory.action;

import io.github.thegatesdev.actionable.factory.ActionFactory;
import io.github.thegatesdev.actionable.util.twin.Twin;
import io.github.thegatesdev.mapletree.data.ReadableOptions;
import io.github.thegatesdev.mapletree.registry.FactoryRegistry;
import io.github.thegatesdev.mapletree.registry.Identifiable;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

import static io.github.thegatesdev.actionable.Actionable.VECTOR;
import static io.github.thegatesdev.actionable.Factories.*;

public final class EntityLocationActions extends FactoryRegistry<Consumer<Twin<Entity, Location>>, ActionFactory<Twin<Entity, Location>>> {
    public EntityLocationActions(String id) {
        super(id, Identifiable::id);
    }

    @Override
    public void registerStatic() {
        register(ActionFactory.multipleFactory(ENTITY_LOCATION_ACTION));
        register(ActionFactory.ifElseFactory(ENTITY_LOCATION_CONDITION, ENTITY_LOCATION_ACTION));
        register(ActionFactory.splitFactory(ENTITY_ACTION, LOCATION_ACTION));
        register(ActionFactory.loopFactory(ENTITY_LOCATION_ACTION));
        register(ActionFactory.loopWhileFactory(ENTITY_LOCATION_ACTION, ENTITY_LOCATION_CONDITION));

        register(new ActionFactory<>("teleport_to", (data, twin) -> twin.actor().teleport(twin.target().setDirection(twin.actor().getLocation().getDirection()).add(data.get("offset", Vector.class))), new ReadableOptions().add("offset", VECTOR, new Vector(0, 0, 0))));

        register(new ActionFactory<>("look_at", (data, twin) -> {
            final Location location = twin.target().clone();
            // Let Location do the direction calculations for us.
            location.setDirection(location.subtract(twin.actor().getLocation()).toVector());
            twin.actor().setRotation(location.getYaw(), location.getPitch());
        }));

        register(new ActionFactory<>("run_world_action", (data, twin) -> data.<Consumer<World>>getUnsafe("action").accept(twin.target().getWorld()), new ReadableOptions()
                .add("action", WORLD_ACTION)
        ));

        register(new ActionFactory<>("offset", (data, twin) -> data.<Consumer<Twin<Entity, Location>>>getUnsafe("action").accept(Twin.of(twin.actor(), twin.target().clone().add(data.<Vector>getUnsafe("offset")))), new ReadableOptions()
                .add("offset", VECTOR)
                .add("action", ENTITY_LOCATION_ACTION)
        ));
    }
}
