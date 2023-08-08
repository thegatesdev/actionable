package io.github.thegatesdev.actionable.builder.condition;

import io.github.thegatesdev.actionable.builder.ConditionBuilder;
import io.github.thegatesdev.actionable.registry.BuilderRegistry;
import io.github.thegatesdev.threshold.util.twin.Twin;
import org.bukkit.entity.Entity;

import java.util.function.Predicate;

import static io.github.thegatesdev.actionable.registry.Registries.ENTITY_CONDITION;
import static io.github.thegatesdev.actionable.registry.Registries.ENTITY_ENTITY_CONDITION;

public final class EntityEntityConditions extends BuilderRegistry.Static<Predicate<Twin<Entity, Entity>>, ConditionBuilder<Twin<Entity, Entity>>> {
    public EntityEntityConditions(String id) {
        super(id);
    }

    @Override
    public void registerStatic() {
        register(ConditionBuilder.orFactory(ENTITY_ENTITY_CONDITION));
        register(ConditionBuilder.andFactory(ENTITY_ENTITY_CONDITION));
        register(ConditionBuilder.flippedFactory(ENTITY_ENTITY_CONDITION));
        register(ConditionBuilder.splitAndFactory(ENTITY_CONDITION, ENTITY_CONDITION));
        register(ConditionBuilder.splitOrFactory(ENTITY_CONDITION, ENTITY_CONDITION));
    }
}
