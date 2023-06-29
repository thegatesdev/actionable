package io.github.thegatesdev.actionable.factory.condition;

import io.github.thegatesdev.actionable.factory.ConditionFactory;
import io.github.thegatesdev.maple.read.ReadableOptions;
import io.github.thegatesdev.maple.registry.StaticFactoryRegistry;
import io.github.thegatesdev.maple.registry.struct.Identifiable;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.function.Predicate;

import static io.github.thegatesdev.actionable.Actionable.VECTOR;
import static io.github.thegatesdev.actionable.Factories.LOCATION_CONDITION;
import static io.github.thegatesdev.actionable.Factories.WORLD_CONDITION;

public final class WorldConditions extends StaticFactoryRegistry<Predicate<World>, ConditionFactory<World>> {
    public WorldConditions(String id) {
        super(id, Identifiable::id);
        info().description("A condition tested on a world.");
    }

    @Override
    public void registerStatic() {
        register(ConditionFactory.andFactory(WORLD_CONDITION));
        register(ConditionFactory.orFactory(WORLD_CONDITION));
        register(new ConditionFactory<>("is_clear_weather", (data, world) -> world.isClearWeather()));
        register(new ConditionFactory<>("run_at", (data, world) ->
                data.<Predicate<Location>>getUnsafe("condition").test(data.getUnsafe("location")),
                new ReadableOptions()
                        .add("condition", LOCATION_CONDITION)
                        .add("location", VECTOR)));
    }
}
