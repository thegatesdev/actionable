package io.github.thegatesdev.actionable.builder;

import io.github.thegatesdev.actionable.registry.DataBuilder;
import io.github.thegatesdev.maple.data.DataMap;
import io.github.thegatesdev.maple.data.DataValue;
import io.github.thegatesdev.maple.data.Keyed;
import io.github.thegatesdev.maple.read.Readable;
import io.github.thegatesdev.maple.read.ReadableOptions;
import io.github.thegatesdev.maple.read.struct.DataTypeHolder;
import io.github.thegatesdev.threshold.event.listening.ClassListener;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

public class ReactorBuilder<E extends Event> implements DataBuilder<ClassListener<E>>, Keyed {
    private final String key;

    private final Class<E> eventClass;
    private final ReadableOptions options;
    private final List<DataEntry<E, ?>> dataEntries = new ArrayList<>();

    private final List<BiPredicate<DataMap, E>> staticConditions = new ArrayList<>();
    private final List<BiConsumer<DataMap, E>> staticActions = new ArrayList<>();

    public ReactorBuilder(String key, Class<E> eventClass) {
        this(key, eventClass, new ReadableOptions());
    }

    public ReactorBuilder(String key, Class<E> eventClass, ReadableOptions readableOptions) {
        this.key = key;
        this.eventClass = eventClass;
        this.options = readableOptions;
        options.add("cancel", Readable.bool(), false);
    }

    // -- BUILD

    @Override
    public ClassListener<E> build(DataMap data) {
        return new Reactor(data, buildEntries(data));
    }

    @SuppressWarnings("unchecked")
    private ReactorEntry<E, ?>[] buildEntries(DataMap data) {
        ReactorEntry<E, ?>[] entries = new ReactorEntry[dataEntries.size()];

        for (int i = 0; i < dataEntries.size(); i++)
            entries[i] = dataEntries.get(i).read(data);
        return entries;
    }

    // -- MUTATE

    public <Data> ReactorBuilder<E> reactor(String name, Function<E, Data> dataGetter, DataTypeHolder<DataValue<Predicate<Data>>> conditionDataType, DataTypeHolder<DataValue<Consumer<Data>>> actionDatatype) {
        var conditionName = name + "_condition";
        var actionName = name + "_action";
        dataEntries.add(new DataEntry<>(conditionName, actionName, dataGetter));
        // Make readableOptions handle dataType reading
        options.addOptional(conditionName, conditionDataType);
        options.addOptional(actionName, actionDatatype);
        return this;
    }

    public ReactorBuilder<E> condition(BiPredicate<DataMap, E> condition) {
        staticConditions.add(condition);
        return this;
    }

    public ReactorBuilder<E> action(BiConsumer<DataMap, E> action) {
        staticActions.add(action);
        return this;
    }

    // -- GET / SET

    @Override
    public String key() {
        return key;
    }

    @Override
    public ReadableOptions readableOptions() {
        return options;
    }

    public Class<E> eventClass() {
        return eventClass;
    }

    // -- CLASS

    private final class Reactor implements ClassListener<E> {
        private final DataMap data;
        private final ReactorEntry<E, ?>[] entries;
        private final boolean cancel;

        public Reactor(DataMap data, ReactorEntry<E, ?>[] entries) {
            this.data = data;
            this.entries = entries;
            this.cancel = data.getBoolean("cancel");
        }

        public boolean test(E event) {
            for (int i = 0; i < staticConditions.size(); i++)
                if (!staticConditions.get(i).test(data, event)) return false;
            for (int i = 0; i < entries.length; i++) if (!entries[i].test(event)) return false;
            return true;
        }

        public void run(E event) {
            for (int i = 0; i < staticActions.size(); i++) staticActions.get(i).accept(data, event);
            for (int i = 0; i < entries.length; i++) entries[i].run(event);
        }

        @Override
        public void onEvent(E event) {
            if (test(event)) {
                run(event);
                if (cancel && event instanceof Cancellable c) c.setCancelled(true);
            }
        }

        @Override
        public Class<E> eventType() {
            return ReactorBuilder.this.eventClass;
        }
    }

    private record ReactorEntry<E extends Event, Data>(Function<E, Data> dataGetter,
                                                       Predicate<Data> condition,
                                                       Consumer<Data> action) {
        public boolean test(E event) {
            return condition.test(dataGetter.apply(event));
        }

        public void run(E event) {
            action.accept(dataGetter.apply(event));
        }

    }

    private record DataEntry<E extends Event, Data>(String conditionName, String actionName,
                                                    Function<E, Data> dataGetter) {
        private ReactorEntry<E, Data> read(DataMap data) {
            return new ReactorEntry<>(dataGetter, data.getUnsafe(conditionName), data.getUnsafe(actionName));
        }
    }
}
