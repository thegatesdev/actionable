package io.github.thegatesdev.actionable.factory.condition;

import io.github.thegatesdev.actionable.factory.ConditionFactory;
import io.github.thegatesdev.actionable.util.twin.Twin;
import io.github.thegatesdev.mapletree.registry.FactoryRegistry;
import io.github.thegatesdev.mapletree.registry.Identifiable;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.function.Predicate;

import static io.github.thegatesdev.actionable.Factories.*;

public final class EntityLocationConditions extends FactoryRegistry<Predicate<Twin<Entity, Location>>, ConditionFactory<Twin<Entity, Location>>> {
    public EntityLocationConditions(String id) {
        super(id, Identifiable::id);
    }

    @Override
    public void registerStatic() {
        register(ConditionFactory.andFactory(ENTITY_LOCATION_CONDITION));
        register(ConditionFactory.orFactory(ENTITY_LOCATION_CONDITION));
        register(ConditionFactory.splitAndFactory(ENTITY_CONDITION, LOCATION_CONDITION));
        register(ConditionFactory.splitOrFactory(ENTITY_CONDITION, LOCATION_CONDITION));
    }
}
