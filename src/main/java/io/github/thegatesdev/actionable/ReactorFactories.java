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

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

// Store all eventFactories. Allow multiple modification through factoriesOf.
public class ReactorFactories implements Identifiable, DataTypeHolder<ReactorFactory<?>.ReadReactor> {
    private final Map<EventType<?>, ReactorFactory<?>> classMapped = new HashMap<>();
    private final Map<Class<? extends Event>, List<?>> factoriesOfCache = new HashMap<>();
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
    public final <E extends Event> ReactorFactory<E> getFactory(EventType<E> eventType) {
        if (eventType == null) return null;
        return (ReactorFactory<E>) classMapped.computeIfAbsent(eventType, (t) -> new ReactorFactory<>(eventType));
    }

    public final ReactorFactory<?> getFactory(String eventId) {
        return getFactory(eventTypes.get(eventId));
    }

    // -- MASS ACTION

    @SuppressWarnings("unchecked")
    @Nullable
    protected final <E extends Event> List<ReactorFactory<? extends E>> factoriesOf(Class<E> baseEventClass) {
        List<ReactorFactory<? extends E>> output;
        {
            output = (List<ReactorFactory<? extends E>>) factoriesOfCache.get(baseEventClass);
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
            output.add((ReactorFactory<? extends E>) getFactory(type));
        factoriesOfCache.put(baseEventClass, output);
        return output;
    }

    public final <E extends Event> void eachFactory(Class<E> baseEventType, Consumer<ReactorFactory<? extends E>> consumer) {
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
    public final DataType<ReactorFactory<?>.ReadReactor> dataType() {
        return dataType;
    }

    @Override
    public final String id() {
        return "event";
    }
}
