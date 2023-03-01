package io.github.thegatesdev.actionable.factory.condition;

import io.github.thegatesdev.actionable.factory.ConditionFactory;
import io.github.thegatesdev.actionable.Factories;
import io.github.thegatesdev.mapletree.data.Readable;
import io.github.thegatesdev.mapletree.data.ReadableOptions;
import io.github.thegatesdev.mapletree.registry.FactoryRegistry;
import io.github.thegatesdev.mapletree.registry.Identifiable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.function.Predicate;

public final class EntityConditions extends FactoryRegistry<Predicate<Entity>, ConditionFactory<Entity>> {
    public EntityConditions(String id) {
        super(id, Identifiable::id);
    }

    @Override
    public void registerStatic() {
        register(ConditionFactory.orFactory(Factories.ENTITY_CONDITION));
        register(ConditionFactory.andFactory(Factories.ENTITY_CONDITION));

        register(new ConditionFactory<>("is_of", (data, entity) -> entity.getType() == data.get("entity_type", EntityType.class),
                new ReadableOptions().add("entity_type", Readable.enumeration(EntityType.class))
        ));

        register(new ConditionFactory<>("on_ground", (data, entity) -> entity.isOnGround()));

        register(new ConditionFactory<>("in_water", (data, entity) -> entity.isInWater()));
    }
}
