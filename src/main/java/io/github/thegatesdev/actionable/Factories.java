package io.github.thegatesdev.actionable;

import io.github.thegatesdev.actionable.factory.action.*;
import io.github.thegatesdev.actionable.factory.condition.*;
import io.github.thegatesdev.mapletree.registry.FactoryRegistry;

import java.util.*;

public class Factories {
    private static boolean locked = false;

    private static final Map<String, FactoryRegistry<?, ?>> FACTORY_REGISTRIES = new HashMap<>();
    private static final Map<String, FactoryRegistry<?, ?>> VIEW = Collections.unmodifiableMap(FACTORY_REGISTRIES);

    public static <F extends FactoryRegistry<?, ?>> F add(F f) {
        if (locked) throw new RuntimeException("Factories are locked");
        FACTORY_REGISTRIES.putIfAbsent(f.id(), f);
        return f;
    }

    public static Set<String> keys() {
        return VIEW.keySet();
    }

    public static Collection<FactoryRegistry<?, ?>> values() {
        return VIEW.values();
    }

    public static FactoryRegistry<?, ?> get(String key) {
        return FACTORY_REGISTRIES.get(key);
    }

    static void lock() {
        locked = true;
    }

    static void registerAll() {
        if (locked) throw new RuntimeException("Factories are locked");
        for (final FactoryRegistry<?, ?> staticRegistration : FACTORY_REGISTRIES.values()) staticRegistration.registerStatic();
    }

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
