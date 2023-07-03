package io.github.thegatesdev.actionable.builder.condition;

import io.github.thegatesdev.actionable.builder.ConditionBuilder;
import io.github.thegatesdev.actionable.registry.BuilderRegistry;
import io.github.thegatesdev.maple.read.ReadableOptions;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.function.Predicate;

import static io.github.thegatesdev.actionable.Actionable.VECTOR;
import static io.github.thegatesdev.actionable.registry.Registries.LOCATION_CONDITION;
import static io.github.thegatesdev.actionable.registry.Registries.WORLD_CONDITION;

public final class WorldConditions extends BuilderRegistry.Static<Predicate<World>, ConditionBuilder<World>> {
    public WorldConditions(String id) {
        super(id);
        info().description("A condition tested on a world.");
    }

    @Override
    public void registerStatic() {
        register(ConditionBuilder.andFactory(WORLD_CONDITION));
        register(ConditionBuilder.orFactory(WORLD_CONDITION));
        register(new ConditionBuilder<>("is_clear_weather", (data, world) -> world.isClearWeather()));
        register(new ConditionBuilder<>("run_at", (data, world) ->
                data.<Predicate<Location>>getUnsafe("condition").test(data.getUnsafe("location")),
                new ReadableOptions()
                        .add("condition", LOCATION_CONDITION)
                        .add("location", VECTOR)));
    }
}
