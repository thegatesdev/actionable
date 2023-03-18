package io.github.thegatesdev.actionable.factory;

import io.github.thegatesdev.eventador.EventManager;
import io.github.thegatesdev.eventador.Reactor;
import io.github.thegatesdev.maple.data.DataMap;
import io.github.thegatesdev.mapletree.data.DataTypeHolder;
import io.github.thegatesdev.mapletree.data.Factory;
import io.github.thegatesdev.mapletree.data.ReadableOptions;
import io.github.thegatesdev.mapletree.data.ReadableOptionsHolder;
import io.github.thegatesdev.mapletree.registry.Identifiable;
import io.github.thegatesdev.threshold.Threshold;
import org.bukkit.event.Event;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.*;

public class ReactorFactory<E extends Event> implements Identifiable, Factory<ReactorFactory<E>.ReadReactor>, ReadableOptionsHolder {
    private final Class<E> eventClass;
    private final ReadableOptions readableOptions;
    private final String id;

    private List<BiPredicate<DataMap, E>> staticConditions;
    private List<BiConsumer<DataMap, E>> staticActions;
    private List<PerformerFactory<?>> performerFactories;

    public ReactorFactory(Class<E> eventClass) {
        this(eventClass, new ReadableOptions());
    }

    public ReactorFactory(Class<E> eventClass, ReadableOptions readableOptions) {
        this.id = EventManager.eventId(eventClass);
        this.eventClass = eventClass;
        this.readableOptions = readableOptions;
    }

    public Class<E> getEventClass() {
        return eventClass;
    }

    public List<PerformerFactory<?>> getPerformerFactories() {
        return Collections.unmodifiableList(performerFactories);
    }

    public ReadReactor build(DataMap data) {
        final DataMap readData = readableOptions == null ? new DataMap() : readableOptions.read(data);
        final List<PerformerFactory<?>.Performer> performers = new LinkedList<>();
        if (performerFactories != null)
            for (PerformerFactory<?> factory : this.performerFactories) {
                final PerformerFactory<?>.Performer performer = factory.create(readData);
                if (performer != null) performers.add(performer);
            }
        final ReadReactor reactor = new ReadReactor(readData, performers);
        reactor.cancelEvent(data.getBoolean("cancel", false));
        return reactor;
    }


    public <D> ReactorFactory<E> addPerformer(String name, Predicate<E> eventPredicate, Function<E, D> dataGetter, DataTypeHolder<? extends Predicate<D>> cD, DataTypeHolder<? extends Consumer<D>> aD) {
        if (performerFactories == null) performerFactories = new ArrayList<>(1);
        final PerformerFactory<D> factory = new PerformerFactory<>(name, dataGetter, eventPredicate);
        readableOptions.add(factory.conditionKey, cD, null);
        readableOptions.add(factory.actionKey, aD, null);
        performerFactories.add(factory);
        return this;
    }

    public <D> ReactorFactory<E> addPerformer(String name, Function<E, D> dataGetter, DataTypeHolder<? extends Predicate<D>> cD, DataTypeHolder<? extends Consumer<D>> aD) {
        return addPerformer(name, null, dataGetter, cD.dataType(), aD.dataType());
    }

    public <D> ReactorFactory<E> addPerformer(String name, Iterable<? extends Function<E, D>> dataGetters, DataTypeHolder<? extends Predicate<D>> cD, DataTypeHolder<? extends Consumer<D>> aD) {
        for (final Function<E, D> getter : dataGetters) {
            addPerformer(name, null, getter, cD.dataType(), aD.dataType());
        }
        return this;
    }

    public ReactorFactory<E> addStaticCondition(BiPredicate<DataMap, E> condition) {
        if (staticConditions == null) staticConditions = new ArrayList<>(1);
        staticConditions.add(condition);
        return this;
    }

    public ReactorFactory<E> addStaticAction(BiConsumer<DataMap, E> action) {
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
    public class ReadReactor implements Reactor<E> {
        private final DataMap data;
        private final List<PerformerFactory<?>.Performer> actionPerformers = new ArrayList<>(), conditionPerformers = new ArrayList<>();

        private boolean cancelEvent = false;

        public ReadReactor(DataMap data, List<PerformerFactory<?>.Performer> performers) {
            this.data = data;
            for (PerformerFactory<?>.Performer performer : performers) {
                if (performer.hasAction) actionPerformers.add(performer);
                if (performer.hasCondition) conditionPerformers.add(performer);
            }
        }

        private boolean checkConditions(E event, Class<E> type) {
            if (staticConditions != null && !Threshold.forEachAND(staticConditions, p -> p.test(data, event)))
                return false;
            return conditionPerformers.isEmpty() || Threshold.forEachAND(conditionPerformers, p -> p.test(event));
        }

        @Override
        public boolean onEvent(E event, Class<E> type) {
            if (!checkConditions(event, type)) return false;
            if (staticActions != null && !staticActions.isEmpty()) {
                for (BiConsumer<DataMap, E> action : staticActions) {
                    action.accept(data, event);
                }
            }
            if (!actionPerformers.isEmpty()) {
                for (PerformerFactory<?>.Performer p : actionPerformers) {
                    p.accept(event);
                }
            }
            return cancelEvent;
        }

        public void cancelEvent(boolean cancelEvent) {
            this.cancelEvent = cancelEvent;
        }

        public Class<E> eventClass() {
            return ReactorFactory.this.eventClass;
        }

        public ReactorFactory<E> getFactory() {
            return ReactorFactory.this;
        }

        @Override
        public int hashCode() {
            int result = data.hashCode();
            result = 31 * result + (cancelEvent ? 1 : 0);
            return result;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ReactorFactory<?>.PerformerFactory<?> that)) return false;
            if (hasPredicate != that.hasPredicate) return false;
            if (!dataGetter.equals(that.dataGetter)) return false;
            if (!Objects.equals(eventPredicate, that.eventPredicate))
                return false;
            if (!actionKey.equals(that.actionKey)) return false;
            return conditionKey.equals(that.conditionKey);
        }

        @Override
        public int hashCode() {
            int result = dataGetter.hashCode();
            result = 31 * result + (eventPredicate != null ? eventPredicate.hashCode() : 0);
            result = 31 * result + actionKey.hashCode();
            result = 31 * result + conditionKey.hashCode();
            result = 31 * result + (hasPredicate ? 1 : 0);
            return result;
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

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof ReactorFactory<?>.PerformerFactory<?>.Performer performer)) return false;
                if (!Objects.equals(action, performer.action)) return false;
                return Objects.equals(condition, performer.condition);
            }

            @Override
            public int hashCode() {
                int result = action != null ? action.hashCode() : 0;
                result = 31 * result + (condition != null ? condition.hashCode() : 0);
                return result;
            }
        }
    }
}