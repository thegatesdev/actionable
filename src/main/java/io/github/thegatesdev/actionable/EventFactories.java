package io.github.thegatesdev.actionable;

import io.github.thegatesdev.actionable.factory.EventFactory;
import io.github.thegatesdev.eventador.core.EventType;
import io.github.thegatesdev.eventador.core.EventTypes;
import io.github.thegatesdev.maple.exception.ElementException;
import io.github.thegatesdev.mapletree.data.DataType;
import io.github.thegatesdev.mapletree.data.DataTypeHolder;
import io.github.thegatesdev.mapletree.data.Readable;
import io.github.thegatesdev.mapletree.registry.Identifiable;
import org.bukkit.event.Event;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class EventFactories implements Identifiable, DataTypeHolder<EventFactory<?>.ReadPerformers> {
    private final Map<EventType<?>, EventFactory<?>> classMapped = new HashMap<>();
    private final Map<Class<? extends Event>, List<?>> factoriesOfCache = new HashMap<>();
    protected final EventTypes eventTypes;

    private final Readable<EventFactory<?>.ReadPerformers> dataType;

    public EventFactories(EventTypes eventTypes) {
        this.eventTypes = eventTypes;
        dataType = Readable.map("event", data -> {
            final String s = data.getString("type");
            final EventFactory<?> factory = getFactory(s);
            if (factory == null)
                throw new ElementException(data, "Specified event %s does not exist".formatted(s));
            return factory.build(data);
        });
        dataType.info().description("Multiple actions and conditions that can be executed when an event happens.");
    }

    // -- FACTORY GETTERS

    @SuppressWarnings("unchecked")
    public final <E extends Event> EventFactory<E> getFactory(EventType<E> eventType) {
        if (eventType == null) return null;
        return (EventFactory<E>) classMapped.computeIfAbsent(eventType, (t) -> new EventFactory<>(eventType));
    }

    public final EventFactory<?> getFactory(String eventId) {
        return getFactory(eventTypes.get(eventId));
    }

    // -- MASS ACTION

    @SuppressWarnings("unchecked")
    @Nullable
    protected final <E extends Event> List<EventFactory<? extends E>> factoriesOf(Class<E> baseEventClass) {
        List<EventFactory<? extends E>> output;
        {
            output = (List<EventFactory<? extends E>>) factoriesOfCache.get(baseEventClass);
            if (output != null) return output;
            else if (factoriesOfCache.containsKey(baseEventClass)) return null;
        }
        final List<EventType<? extends E>> types = eventTypes.listenableEvents(baseEventClass);
        if (types == null) {
            factoriesOfCache.put(baseEventClass, null);
            return null;
        }
        output = new ArrayList<>(types.size());
        for (EventType<?> type : types)
            output.add((EventFactory<? extends E>) getFactory(type));
        final var nomod = Collections.unmodifiableList(output);
        factoriesOfCache.put(baseEventClass, nomod);
        return nomod;
    }

    public final <E extends Event> void eachFactory(Class<E> baseEventType, Consumer<EventFactory<? extends E>> consumer) {
        final var factoriesOf = factoriesOf(baseEventType);
        if (factoriesOf != null) factoriesOf.forEach(consumer);
    }


    // -- GETTERS

    public final Collection<String> keys() {
        final List<String> list = new ArrayList<>(classMapped.keySet().size());
        for (EventType<?> type : classMapped.keySet()) list.add(type.name());
        return list;
    }

    @Override
    public final DataType<EventFactory<?>.ReadPerformers> dataType() {
        return dataType;
    }

    @Override
    public final String id() {
        return "event";
    }
}
