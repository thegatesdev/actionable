package io.github.thegatesdev.actionable.factory;

import io.github.thegatesdev.eventador.core.EventType;
import io.github.thegatesdev.eventador.listener.EventTypeHolder;
import io.github.thegatesdev.eventador.listener.StaticListener;
import io.github.thegatesdev.maple.data.DataMap;
import io.github.thegatesdev.mapletree.data.DataTypeHolder;
import io.github.thegatesdev.mapletree.data.Factory;
import io.github.thegatesdev.mapletree.data.ReadableOptions;
import io.github.thegatesdev.mapletree.data.ReadableOptionsHolder;
import io.github.thegatesdev.mapletree.registry.Identifiable;
import org.bukkit.event.Event;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.*;

public class EventFactory<E extends Event> implements Identifiable, Factory<EventFactory<E>.ReadPerformers>, ReadableOptionsHolder, EventTypeHolder<E> {
    private final EventType<E> eventType;
    private final ReadableOptions readableOptions;
    private final String id;

    private List<BiPredicate<DataMap, E>> staticConditions;
    private List<BiConsumer<DataMap, E>> staticActions;
    private final List<PerformerFactory<?>> performerFactories = new ArrayList<>();
    private final Set<String> performerNames = new HashSet<>();

    public EventFactory(EventType<E> eventType) {
        this(eventType, new ReadableOptions());
    }

    public EventFactory(EventType<E> eventType, ReadableOptions readableOptions) {
        this.id = eventType.name();
        this.eventType = eventType;
        this.readableOptions = readableOptions;
    }

    public EventType<E> eventType() {
        return eventType;
    }

    public ReadPerformers build(DataMap data) {
        final DataMap readData = readableOptions == null ? data : readableOptions.read(data);
        final List<PerformerFactory<?>.Performer> performers = new ArrayList<>();
        for (PerformerFactory<?> factory : this.performerFactories) {
            final PerformerFactory<?>.Performer performer = factory.create(readData);
            if (performer != null) performers.add(performer);
        }
        final ReadPerformers performer = new ReadPerformers(readData, performers);
        performer.cancelEvent(data.getBoolean("cancel", false));
        return performer;
    }


    public <D> EventFactory<E> addPerformer(String name, Function<E, D> dataGetter, Predicate<E> eventPredicate, DataTypeHolder<Predicate<D>> cD, DataTypeHolder<Consumer<D>> aD) {
        if (!performerNames.add(name))
            throw new IllegalArgumentException("Performer by the name of %s already exists for event %s".formatted(name, eventType.name()));
        final PerformerFactory<D> factory = new PerformerFactory<>(name, dataGetter, eventPredicate);
        readableOptions.add(factory.conditionKey, cD, null);
        readableOptions.add(factory.actionKey, aD, null);
        performerFactories.add(factory);
        return this;
    }

    public <D> EventFactory<E> addPerformer(String name, Function<E, D> dataGetter, DataTypeHolder<Predicate<D>> cD, DataTypeHolder<Consumer<D>> aD) {
        return addPerformer(name, dataGetter, null, cD.dataType(), aD.dataType());
    }

    public EventFactory<E> addStaticCondition(BiPredicate<DataMap, E> condition) {
        if (staticConditions == null) staticConditions = new ArrayList<>(1);
        staticConditions.add(condition);
        return this;
    }

    public EventFactory<E> addStaticAction(BiConsumer<DataMap, E> action) {
        if (staticActions == null) staticActions = new ArrayList<>(1);
        staticActions.add(action);
        return this;
    }

    @Nonnull
    @Override
    public ReadableOptions readableOptions() {
        return readableOptions;
    }

    @Override
    public String id() {
        return id;
    }

    // A list of read performers for this event,
    public class ReadPerformers implements StaticListener<E>, EventTypeHolder<E> {
        private final DataMap data;
        private final List<PerformerFactory<?>.Performer> actionPerformers = new ArrayList<>(), conditionPerformers = new ArrayList<>();

        private boolean cancelEvent = false;

        public ReadPerformers(DataMap data, List<PerformerFactory<?>.Performer> performers) {
            this.data = data;
            for (PerformerFactory<?>.Performer performer : performers) {
                if (performer.hasAction) actionPerformers.add(performer);
                if (performer.hasCondition) conditionPerformers.add(performer);
            }
        }

        public boolean checkConditions(E event) {
            // Check static conditions
            if (staticConditions != null)
                for (int i = 0, staticConditionsSize = staticConditions.size(); i < staticConditionsSize; i++)
                    if (!staticConditions.get(i).test(data, event)) return false;
            // Check performer conditions
            for (int i = 0, conditionPerformersSize = conditionPerformers.size(); i < conditionPerformersSize; i++)
                if (!conditionPerformers.get(i).test(event)) return false;
            return true;
        }

        public void run(E event) {
            // Run static actions
            if (staticActions != null)
                for (int i = 0, staticActionsSize = staticActions.size(); i < staticActionsSize; i++)
                    staticActions.get(i).accept(data, event);
            // Run performer actions
            if (!actionPerformers.isEmpty())
                for (int i = 0, actionPerformersSize = actionPerformers.size(); i < actionPerformersSize; i++)
                    actionPerformers.get(i).accept(event);
        }

        public void cancelEvent(boolean cancelEvent) {
            this.cancelEvent = eventType.cancellable() && cancelEvent;
        }

        @Override
        public void callEvent(@Nonnull final E event, @Nonnull final EventType<E> eventType) {
            if (checkConditions(event)) {
                run(event);
                if (cancelEvent) eventType.cancelEvent(event);
            }
        }

        @Override
        public EventType<E> eventType() {
            return EventFactory.this.eventType;
        }

        public EventFactory<E> getFactory() {
            return EventFactory.this;
        }
    }

    // Performer test and runs a condition and action on a certain type of data ( D )
    public class PerformerFactory<D> {
        private final Function<E, D> dataGetter;
        private final Predicate<E> eventPredicate;
        private final String actionKey, conditionKey;
        private final boolean hasPredicate;

        private PerformerFactory(String name, Function<E, D> dataGetter, Predicate<E> eventPredicate) {
            this.dataGetter = dataGetter;
            this.actionKey = name + "_action";
            this.conditionKey = name + "_condition";
            this.eventPredicate = eventPredicate;
            hasPredicate = eventPredicate != null;
        }

        private PerformerFactory(String name, Function<E, D> dataGetter) {
            this(name, dataGetter, null);
        }

        public Performer create(DataMap data) {
            final Consumer<D> action = data.getUnsafe(actionKey, null);
            final Predicate<D> condition = data.getUnsafe(conditionKey, null);
            if (action == null && condition == null) return null;
            return new Performer(action, condition);
        }

        public class Performer implements Predicate<E>, Consumer<E> {
            private final Consumer<D> action;
            private final Predicate<D> condition;
            private final boolean hasAction, hasCondition;
            private D prevData = null; // Data gotten by the condition check, reuse in the action run.

            private Performer(Consumer<D> action, Predicate<D> condition) {
                this.condition = condition;
                this.hasCondition = condition != null;
                this.action = action;
                this.hasAction = action != null;
            }

            public boolean test(E event) {
                if (hasPredicate && !eventPredicate.test(event)) return false;
                final D data = dataGetter.apply(event);
                if (data == null) return false;
                if (condition.test(data)) {
                    prevData = data;
                    return true;
                }
                return false;
            }

            public void accept(E event) {
                if (!hasCondition) prevData = dataGetter.apply(event);
                if (prevData != null)   // Can still be null since checkCondition being false doesn't guarantee run won't get called.
                    action.accept(prevData);
            }
        }
    }
}