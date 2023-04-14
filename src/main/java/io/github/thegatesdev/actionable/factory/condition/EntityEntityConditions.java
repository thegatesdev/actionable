package io.github.thegatesdev.actionable.factory.condition;

import io.github.thegatesdev.actionable.Factories;
import io.github.thegatesdev.actionable.factory.ConditionFactory;
import io.github.thegatesdev.actionable.util.twin.Twin;
import io.github.thegatesdev.mapletree.registry.FactoryRegistry;
import io.github.thegatesdev.mapletree.registry.Identifiable;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.function.Predicate;

public final class EntityEntityConditions extends FactoryRegistry<Predicate<Twin<Entity, Entity>>, ConditionFactory<Twin<Entity, Entity>>> {
    public EntityEntityConditions(String id) {
        super(id, Identifiable::id);
    }

    @Override
    public void registerStatic() {
        register(ConditionFactory.orFactory(Factories.ENTITY_ENTITY_CONDITION));
        register(ConditionFactory.andFactory(Factories.ENTITY_ENTITY_CONDITION));
        register(ConditionFactory.flippedFactory(Factories.ENTITY_ENTITY_CONDITION));
        register(ConditionFactory.splitAndFactory(Factories.ENTITY_CONDITION, Factories.ENTITY_CONDITION));
        register(ConditionFactory.splitOrFactory(Factories.ENTITY_CONDITION, Factories.ENTITY_CONDITION));

        register(new ConditionFactory<>("can_see", (data, twin) -> {
            final Location actorLoc = twin.actor().getLocation();
            return twin.actor().getWorld().rayTraceBlocks(actorLoc, twin.target().getLocation().subtract(actorLoc).toVector().normalize(), actorLoc.distanceSquared(twin.target().getLocation()), FluidCollisionMode.NEVER, true) == null;
        }));
    }
}
