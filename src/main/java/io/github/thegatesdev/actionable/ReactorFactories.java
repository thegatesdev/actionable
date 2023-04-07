package io.github.thegatesdev.actionable;

import io.github.thegatesdev.actionable.factory.ReactorFactory;
import io.github.thegatesdev.eventador.core.EventType;
import io.github.thegatesdev.eventador.core.EventTypes;
import io.github.thegatesdev.maple.exception.ElementException;
import io.github.thegatesdev.mapletree.data.DataType;
import io.github.thegatesdev.mapletree.data.DataTypeHolder;
import io.github.thegatesdev.mapletree.data.Readable;
import io.github.thegatesdev.mapletree.registry.Identifiable;
import org.bukkit.event.Event;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

// Store all eventFactories. Allow multiple modification through factoriesOf.
public class ReactorFactories implements Identifiable, DataTypeHolder<ReactorFactory<?>.ReadReactor> {
    private final Map<EventType<?>, ReactorFactory<?>> classMapped = new HashMap<>();
    private final Map<EventType<?>, List<?>> factoriesOfCache = new HashMap<>();
    protected final EventTypes eventTypes;

    private final Readable<ReactorFactory<?>.ReadReactor> dataType;

    public ReactorFactories(EventTypes eventTypes) {
        this.eventTypes = eventTypes;
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
    public final <E extends Event> ReactorFactory<E> getFactory(EventType<E> eventClass) {
        if (eventClass == null || !EventTypes.hasHandlerList(eventClass)) return null;
        final ReactorFactory<E> reactorFactory = (ReactorFactory<E>) classMapped.get(eventClass);
        if (reactorFactory != null) return reactorFactory;
        return (ReactorFactory<E>) classMapped.compute(eventClass, (a, c) -> new ReactorFactory<>(eventClass));
    }

    public final ReactorFactory<?> getFactory(String eventId) {
        return getFactory(eventTypes.get(eventId));
    }

    // -- MASS ACTION

    @SuppressWarnings("unchecked")
    protected final <E extends Event> List<ReactorFactory<? extends E>> factoriesOf(EventType<E> baseEventClass) {
        List<ReactorFactory<? extends E>> output;
        {
            output = (List<ReactorFactory<? extends E>>) factoriesOfCache.get(baseEventClass);
            if (output != null) return output;
        }
        final List<EventType<? extends E>> classes = eventTypes.listenableSubEvents(baseEventClass);
        output = new ArrayList<>(classes.size());
        for (EventType<?> clazz : classes)
            output.add((ReactorFactory<? extends E>) getFactory(clazz));
        factoriesOfCache.put(baseEventClass, output);
        return output;
    }

    public final <E extends Event> void subFactories(EventType<E> baseEventType, Consumer<ReactorFactory<? extends E>> consumer) {
        final var eventFactories = factoriesOf(baseEventType);
        if (eventFactories.isEmpty()) throw new RuntimeException("Warning: doWithFactoriesOf did nothing");
        eventFactories.forEach(consumer);
    }

    public final <E extends Event> void subFactories(Class<E> baseEventClass, Consumer<ReactorFactory<? extends E>> consumer) {
        subFactories(eventTypes.get(baseEventClass), consumer);
    }


    @SuppressWarnings("unchecked")
    public final <E extends Event, D> void addPerformers(String name, EventType<E> baseEventType, Predicate<E> eventPredicate, Function<E, D> dataGetter, DataTypeHolder<Predicate<D>> conditionType, DataTypeHolder<Consumer<D>> actionType) {
        for (final ReactorFactory<? extends E> factory : factoriesOf(baseEventType))
            ((ReactorFactory<E>) factory).addPerformer(name, eventPredicate, conditionType, actionType, dataGetter);
    }

    public final <E extends Event, D> void addPerformers(String name, Class<E> baseEventClass, Predicate<E> eventPredicate, Function<E, D> dataGetter, DataTypeHolder<Predicate<D>> conditionType, DataTypeHolder<Consumer<D>> actionType) {
        addPerformers(name, eventTypes.get(baseEventClass), eventPredicate, dataGetter, conditionType, actionType);
    }


    // -- GETTERS

    public final Collection<String> keys() {
        final List<String> list = new ArrayList<>(classMapped.keySet().size());
        for (EventType<?> type : classMapped.keySet()) list.add(type.name());
        return list;
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
