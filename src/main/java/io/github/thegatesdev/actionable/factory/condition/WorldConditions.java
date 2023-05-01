package io.github.thegatesdev.actionable.factory.condition;

import io.github.thegatesdev.actionable.factory.ConditionFactory;
import io.github.thegatesdev.mapletree.registry.FactoryRegistry;
import io.github.thegatesdev.mapletree.registry.Identifiable;
import org.bukkit.World;

import java.util.function.Predicate;

import static io.github.thegatesdev.actionable.Factories.WORLD_CONDITION;

public final class WorldConditions extends FactoryRegistry<Predicate<World>, ConditionFactory<World>> {
    public WorldConditions(String id) {
        super(id, Identifiable::id);
    }

    @Override
    public void registerStatic() {
        register(ConditionFactory.andFactory(WORLD_CONDITION));
        register(ConditionFactory.orFactory(WORLD_CONDITION));
        register(new ConditionFactory<>("is_clear_weather", (data, world) -> world.isClearWeather()));
    }
}
