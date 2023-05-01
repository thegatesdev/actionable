package io.github.thegatesdev.actionable.factory.condition;

import io.github.thegatesdev.actionable.factory.ConditionFactory;
import io.github.thegatesdev.mapletree.data.Readable;
import io.github.thegatesdev.mapletree.data.ReadableOptions;
import io.github.thegatesdev.mapletree.registry.FactoryRegistry;
import io.github.thegatesdev.mapletree.registry.Identifiable;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.function.Predicate;

import static io.github.thegatesdev.actionable.Factories.LOCATION_CONDITION;

public final class LocationConditions extends FactoryRegistry<Predicate<Location>, ConditionFactory<Location>> {
    public LocationConditions(String id) {
        super(id, Identifiable::id);
    }

    @Override
    public void registerStatic() {
        register(ConditionFactory.andFactory(LOCATION_CONDITION));
        register(ConditionFactory.orFactory(LOCATION_CONDITION));
        register(new ConditionFactory<>("is_liquid", (data, location) -> location.getBlock().isLiquid()));
        register(new ConditionFactory<>("is_air", (data, location) -> location.getBlock().isEmpty()));
        register(new ConditionFactory<>("is_full", (data, location) -> location.getBlock().getType().isOccluding()));
        register(new ConditionFactory<>("is_of", (data, location) -> location.getBlock().getType() == data.getUnsafe("material"),
                new ReadableOptions()
                        .add("material", Readable.enumeration(Material.class))
        ));
    }
}
