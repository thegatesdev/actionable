package io.github.thegatesdev.actionable.builder.condition;

import io.github.thegatesdev.actionable.builder.ConditionBuilder;
import io.github.thegatesdev.actionable.registry.BuilderRegistry;
import io.github.thegatesdev.threshold.util.twin.Twin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.function.Predicate;

import static io.github.thegatesdev.actionable.registry.Registries.*;

public final class EntityLocationConditions extends BuilderRegistry.Static<Predicate<Twin<Entity, Location>>, ConditionBuilder<Twin<Entity, Location>>> {
    public EntityLocationConditions(String id) {
        super(id);
    }

    @Override
    public void registerStatic() {
        register(ConditionBuilder.andFactory(ENTITY_LOCATION_CONDITION));
        register(ConditionBuilder.orFactory(ENTITY_LOCATION_CONDITION));
        register(ConditionBuilder.splitAndFactory(ENTITY_CONDITION, LOCATION_CONDITION));
        register(ConditionBuilder.splitOrFactory(ENTITY_CONDITION, LOCATION_CONDITION));
    }
}
