package io.github.thegatesdev.actionable.factory.condition;

import io.github.thegatesdev.actionable.factory.ConditionFactory;
import io.github.thegatesdev.maple.read.ReadableOptions;
import io.github.thegatesdev.maple.registry.StaticFactoryRegistry;
import io.github.thegatesdev.maple.registry.struct.Identifiable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.function.Predicate;

import static io.github.thegatesdev.actionable.Factories.ENTITY_CONDITION;
import static io.github.thegatesdev.maple.read.Readable.enumeration;

public final class EntityConditions extends StaticFactoryRegistry<Predicate<Entity>, ConditionFactory<Entity>> {
    public EntityConditions(String id) {
        super(id, Identifiable::id);
        info().description("A condition tested on a single entity.");
    }

    @Override
    public void registerStatic() {
        register(ConditionFactory.orFactory(ENTITY_CONDITION));
        register(ConditionFactory.andFactory(ENTITY_CONDITION));

        register(new ConditionFactory<>("is_of", (data, entity) -> entity.getType() == data.getObject("entity_type", EntityType.class),
                new ReadableOptions().add("entity_type", enumeration(EntityType.class))
        ));

        register(new ConditionFactory<>("sneaking", (data, entity) -> entity.isSneaking()));

        register(new ConditionFactory<>("frozen", (data, entity) -> entity.isFrozen()));

        register(new ConditionFactory<>("on_ground", (data, entity) -> entity.isOnGround()));

        register(new ConditionFactory<>("in_water", (data, entity) -> entity.isInWater()));

        register(new ConditionFactory<>("under_water", (data, entity) -> entity.isInWater()));

        register(new ConditionFactory<>("in_rain", (data, entity) -> entity.isInWater()));

        register(new ConditionFactory<>("in_lava", (data, entity) -> entity.isInWater()));

        register(new ConditionFactory<>("in_powdered_snow", (data, entity) -> entity.isInWater()));

        register(new ConditionFactory<>("has_passenger", (data, entity) -> !entity.isEmpty()));

        register(new ConditionFactory<>("in_vehicle", (data, entity) -> entity.isInsideVehicle()));
    }
}
