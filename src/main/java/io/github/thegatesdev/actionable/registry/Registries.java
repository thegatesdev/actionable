package io.github.thegatesdev.actionable.registry;

import io.github.thegatesdev.actionable.builder.action.*;
import io.github.thegatesdev.actionable.builder.condition.*;
import io.github.thegatesdev.actionable.builder.reactor.Reactors;

import java.util.*;

public class Registries {
    private static boolean locked = false;

    private static final Map<String, BuilderRegistry<?, ?>> FACTORY_REGISTRIES = new HashMap<>();
    private static final Map<String, BuilderRegistry<?, ?>> VIEW = Collections.unmodifiableMap(FACTORY_REGISTRIES);

    public static <F extends BuilderRegistry<?, ?>> F add(F f) {
        if (locked) throw new RuntimeException("Registries are locked");
        if (FACTORY_REGISTRIES.putIfAbsent(f.key(), f) != null)
            throw new RuntimeException("Duplicate factory entry " + f.key());
        return f;
    }

    public static Set<String> keys() {
        return VIEW.keySet();
    }

    public static Collection<BuilderRegistry<?, ?>> values() {
        return VIEW.values();
    }

    public static BuilderRegistry<?, ?> get(String key) {
        return FACTORY_REGISTRIES.get(key);
    }

    public static void lock() {
        locked = true;
    }

    public static void registerAll() {
        if (locked) throw new RuntimeException("Registries are locked");
        for (final BuilderRegistry<?, ?> builderRegistry : FACTORY_REGISTRIES.values())
            if (builderRegistry instanceof BuilderRegistry.Static<?, ?> stat) stat.registerStatic();
    }

    public static final Reactors REACTORS = add(new Reactors("reactors"));

    public static final EntityActions ENTITY_ACTION = add(new EntityActions("entity_action"));
    public static final WorldActions WORLD_ACTION = add(new WorldActions("world_action"));
    public static final EntityLocationActions ENTITY_LOCATION_ACTION = add(new EntityLocationActions("entity_location_action"));
    public static final LocationActions LOCATION_ACTION = add(new LocationActions("location_action"));
    public static final EntityEntityActions ENTITY_ENTITY_ACTION = add(new EntityEntityActions("entity_entity_action"));

    public static final EntityConditions ENTITY_CONDITION = add(new EntityConditions("entity_condition"));
    public static final WorldConditions WORLD_CONDITION = add(new WorldConditions("world_condition"));
    public static final EntityLocationConditions ENTITY_LOCATION_CONDITION = add(new EntityLocationConditions("entity_location_condition"));
    public static final LocationConditions LOCATION_CONDITION = add(new LocationConditions("location_condition"));
    public static final EntityEntityConditions ENTITY_ENTITY_CONDITION = add(new EntityEntityConditions("entity_entity_condition"));
}
