package io.github.thegatesdev.actionable.builder.condition;

import io.github.thegatesdev.actionable.builder.ConditionBuilder;
import io.github.thegatesdev.actionable.registry.BuilderRegistry;
import io.github.thegatesdev.maple.read.ReadableOptions;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.function.Predicate;

import static io.github.thegatesdev.actionable.registry.Registries.ENTITY_CONDITION;
import static io.github.thegatesdev.maple.read.Readable.enumeration;

public final class EntityConditions extends BuilderRegistry.Static<Predicate<Entity>, ConditionBuilder<Entity>> {
    public EntityConditions(String id) {
        super(id);
        info().description("A condition tested on a single entity.");
    }

    @Override
    public void registerStatic() {
        register(ConditionBuilder.orFactory(ENTITY_CONDITION));
        register(ConditionBuilder.andFactory(ENTITY_CONDITION));

        register(new ConditionBuilder<>("is_of", (data, entity) -> entity.getType() == data.getObject("entity_type", EntityType.class),
            new ReadableOptions().add("entity_type", enumeration(EntityType.class))
        ));

        register(new ConditionBuilder<>("sneaking", (data, entity) -> entity.isSneaking()));

        register(new ConditionBuilder<>("frozen", (data, entity) -> entity.isFrozen()));

        register(new ConditionBuilder<>("on_ground", (data, entity) -> entity.isOnGround()));

        register(new ConditionBuilder<>("in_water", (data, entity) -> entity.isInWater()));

        register(new ConditionBuilder<>("under_water", (data, entity) -> entity.isInWater()));

        register(new ConditionBuilder<>("in_rain", (data, entity) -> entity.isInWater()));

        register(new ConditionBuilder<>("in_lava", (data, entity) -> entity.isInWater()));

        register(new ConditionBuilder<>("in_powdered_snow", (data, entity) -> entity.isInWater()));

        register(new ConditionBuilder<>("has_passenger", (data, entity) -> !entity.isEmpty()));

        register(new ConditionBuilder<>("in_vehicle", (data, entity) -> entity.isInsideVehicle()));
    }
}
