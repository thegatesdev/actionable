package io.github.thegatesdev.actionable.factory.condition;

import io.github.thegatesdev.actionable.factory.ConditionFactory;
import io.github.thegatesdev.actionable.util.twin.Twin;
import io.github.thegatesdev.mapletree.registry.Identifiable;
import io.github.thegatesdev.mapletree.registry.StaticFactoryRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.function.Predicate;

import static io.github.thegatesdev.actionable.Factories.*;

public final class EntityLocationConditions extends StaticFactoryRegistry<Predicate<Twin<Entity, Location>>, ConditionFactory<Twin<Entity, Location>>> {
    public EntityLocationConditions(String id) {
        super(id, Identifiable::id);
        info().description("A condition tested between an actor entity and a target location.");
    }

    @Override
    public void registerStatic() {
        register(ConditionFactory.andFactory(ENTITY_LOCATION_CONDITION));
        register(ConditionFactory.orFactory(ENTITY_LOCATION_CONDITION));
        register(ConditionFactory.splitAndFactory(ENTITY_CONDITION, LOCATION_CONDITION));
        register(ConditionFactory.splitOrFactory(ENTITY_CONDITION, LOCATION_CONDITION));
    }
}
