package io.github.thegatesdev.actionable.builder.condition;

import io.github.thegatesdev.actionable.builder.ConditionBuilder;
import io.github.thegatesdev.actionable.registry.BuilderRegistry;
import io.github.thegatesdev.maple.read.Options;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.function.Predicate;

import static io.github.thegatesdev.actionable.registry.Builders.ENTITY_CONDITION;
import static io.github.thegatesdev.maple.read.Readable.enumeration;

public final class EntityConditions extends BuilderRegistry.Static<Predicate<Entity>, ConditionBuilder<Entity>> {
    public EntityConditions(String id) {
        super(id);
    }

    @Override
    public void registerStatic() {
        register(ConditionBuilder.orFactory(ENTITY_CONDITION));
        register(ConditionBuilder.andFactory(ENTITY_CONDITION));

        register(new ConditionBuilder<>("is_of", (data, entity) -> entity.getType() == data.getObject("entity_type", EntityType.class),
            new Options.Builder().add("entity_type", enumeration(EntityType.class))
        ));

        register(new ConditionBuilder<>("dead", (data, entity) -> entity.isDead()));

        register(new ConditionBuilder<>("sneaking", (data, entity) -> entity.isSneaking()));

        register(new ConditionBuilder<>("frozen", (data, entity) -> entity.isFrozen()));

        register(new ConditionBuilder<>("on_ground", (data, entity) -> entity.isOnGround()));

        register(new ConditionBuilder<>("in_water", (data, entity) -> entity.isInWater()));

        register(new ConditionBuilder<>("under_water", (data, entity) -> entity.isUnderWater()));

        register(new ConditionBuilder<>("in_rain", (data, entity) -> entity.isInRain()));

        register(new ConditionBuilder<>("in_lava", (data, entity) -> entity.isInLava()));

        register(new ConditionBuilder<>("in_powdered_snow", (data, entity) -> entity.isInPowderedSnow()));

        register(new ConditionBuilder<>("has_passenger", (data, entity) -> !entity.isEmpty()));

        register(new ConditionBuilder<>("in_vehicle", (data, entity) -> entity.isInsideVehicle()));
    }
}
