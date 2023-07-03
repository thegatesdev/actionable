package io.github.thegatesdev.actionable.builder.condition;

import io.github.thegatesdev.actionable.builder.ConditionBuilder;
import io.github.thegatesdev.actionable.registry.BuilderRegistry;
import io.github.thegatesdev.maple.read.ReadableOptions;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.function.Predicate;

import static io.github.thegatesdev.actionable.registry.Registries.LOCATION_CONDITION;
import static io.github.thegatesdev.maple.read.Readable.enumeration;

public final class LocationConditions extends BuilderRegistry.Static<Predicate<Location>, ConditionBuilder<Location>> {
    public LocationConditions(String id) {
        super(id);
        info().description("A condition tested on a location in a world.");
    }

    @Override
    public void registerStatic() {
        register(ConditionBuilder.andFactory(LOCATION_CONDITION));
        register(ConditionBuilder.orFactory(LOCATION_CONDITION));
        register(new ConditionBuilder<>("is_liquid", (data, location) -> location.getBlock().isLiquid()));
        register(new ConditionBuilder<>("is_air", (data, location) -> location.getBlock().isEmpty()));
        register(new ConditionBuilder<>("is_full", (data, location) -> location.getBlock().getType().isOccluding()));
        register(new ConditionBuilder<>("is_of", (data, location) -> location.getBlock().getType() == data.getUnsafe("material"),
                new ReadableOptions()
                        .add("material", enumeration(Material.class))
        ));
    }
}
