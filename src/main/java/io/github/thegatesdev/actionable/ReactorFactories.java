package io.github.thegatesdev.actionable;

import io.github.thegatesdev.actionable.factory.ReactorFactory;
import io.github.thegatesdev.eventador.event.EventManager;
import io.github.thegatesdev.eventador.event.util.EventData;
import io.github.thegatesdev.maple.exception.ElementException;
import io.github.thegatesdev.mapletree.data.DataType;
import io.github.thegatesdev.mapletree.data.DataTypeHolder;
import io.github.thegatesdev.mapletree.data.Readable;
import io.github.thegatesdev.mapletree.registry.Identifiable;
import org.bukkit.event.Event;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

// Store all eventFactories. Allow multiple modification through factoriesOf.
public class ReactorFactories implements Identifiable, DataTypeHolder<ReactorFactory<?>.ReadReactor> {
    private final Map<Class<? extends Event>, ReactorFactory<?>> classMapped = new HashMap<>();
    private final Map<Class<? extends Event>, Set<?>> factoriesOfCache = new HashMap<>();
    protected final EventManager eventManager;

    private final Readable<ReactorFactory<?>.ReadReactor> dataType;

    public ReactorFactories(EventManager eventManager) {
        this.eventManager = eventManager;
        dataType = Readable.map("event", data -> {
            final String s = data.getString("type");
            final ReactorFactory<?> factory = getFactory(s);
            if (factory == null)
                throw new ElementException(data, "specified event %s does not exist".formatted(s));
            return factory.build(data);
        });
    }

    // -- FACTORY GETTERS

    @SuppressWarnings("unchecked")
    public final <E extends Event> ReactorFactory<E> getFactory(Class<E> eventClass) {
        if (eventClass == null || !EventManager.hasHandlerList(eventClass)) return null;
        final ReactorFactory<E> reactorFactory = (ReactorFactory<E>) classMapped.get(eventClass);
        if (reactorFactory != null) return reactorFactory;
        return (ReactorFactory<E>) classMapped.compute(eventClass, (a, c) -> new ReactorFactory<>(eventClass));
    }

    public final ReactorFactory<?> getFactory(String eventId) {
        return getFactory(eventManager.eventClass(eventId));
    }

    // -- MASS ACTION

    @SuppressWarnings("unchecked")
    protected final <E extends Event> Set<ReactorFactory<? extends E>> factoriesOf(Class<E> baseEventClass) {
        Set<ReactorFactory<? extends E>> output;
        {
            output = (Set<ReactorFactory<? extends E>>) factoriesOfCache.get(baseEventClass);
            if (output != null) return output;
        }
        final Set<Class<? extends E>> classes = eventManager.allEventsOf(baseEventClass);
        output = new HashSet<>(classes.size());
        for (Class<? extends Event> clazz : classes)
            output.add((ReactorFactory<? extends E>) getFactory(clazz));
        factoriesOfCache.put(baseEventClass, output);
        return output;
    }

    public final <E extends Event> void doWithFactories(Class<E> baseEventClass, Consumer<ReactorFactory<? extends E>> consumer) {
        final var eventFactories = factoriesOf(baseEventClass);
        eventFactories.forEach(consumer);
        if (eventFactories.isEmpty()) throw new RuntimeException("Warning: doWithFactoriesOf did nothing");
    }

    public final <T> void addPerformers(EventData<T> eventData, DataTypeHolder<? extends Consumer<T>> actionType, DataTypeHolder<? extends Predicate<T>> conditionType) {
        for (final Class<? extends Event> aClass : eventData.eventSet()) {
            addPerformersFor(aClass, null, eventData, actionType, conditionType); // Move to other method to have common type ( E )
        }
    }

    @SuppressWarnings("unchecked")
    public final <E extends Event, T> void addPerformers(Class<E> baseEventClass, Predicate<E> eventPredicate, EventData<T> eventData, DataTypeHolder<? extends Consumer<T>> actionType, DataTypeHolder<? extends Predicate<T>> conditionType) {
        for (final Class<? extends Event> aClass : eventData.eventSet()) {
            if (baseEventClass.isAssignableFrom(aClass))
                addPerformersFor((Class<E>) aClass, eventPredicate, eventData, actionType, conditionType);
        }
    }

    private <T, E extends Event> void addPerformersFor(Class<E> eventClass, Predicate<E> eventPredicate, EventData<T> eventData, DataTypeHolder<? extends Consumer<T>> actionType, DataTypeHolder<? extends Predicate<T>> conditionType) {
        final ReactorFactory<E> reactorFactory = getFactory(eventClass);
        eventData.get(eventClass).forEach((s, function) -> reactorFactory.addPerformer(s, eventPredicate, function, conditionType, actionType));
    }

    // -- GETTERS

    public final Set<String> keySet() {
        final Set<String> keys = new LinkedHashSet<>(classMapped.size());
        classMapped.keySet().forEach(aClass -> keys.add(EventManager.eventId(aClass)));
        return keys;
    }

    @Override
    public final DataType<ReactorFactory<?>.ReadReactor> dataType() {
        return dataType;
    }

    @Override
    public final String id() {
        return "event";
    }
}
