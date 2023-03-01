package io.github.thegatesdev.actionable.factory;

import io.github.thegatesdev.actionable.util.twin.Twin;
import io.github.thegatesdev.maple.data.DataMap;
import io.github.thegatesdev.maple.data.DataPrimitive;
import io.github.thegatesdev.mapletree.data.DataTypeHolder;
import io.github.thegatesdev.mapletree.data.Factory;
import io.github.thegatesdev.mapletree.data.ReadableOptions;
import io.github.thegatesdev.mapletree.data.ReadableOptionsHolder;
import io.github.thegatesdev.mapletree.registry.Identifiable;
import io.github.thegatesdev.threshold.Threshold;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ConditionFactory<R> implements Identifiable, Factory<Predicate<R>>, ReadableOptionsHolder {
    private final String id;
    private final BiPredicate<DataMap, R> condition;
    private final ReadableOptions readableOptions;

    public ConditionFactory(String id, BiPredicate<DataMap, R> condition, ReadableOptions readableOptions) {
        this.id = id;
        this.condition = condition;
        this.readableOptions = readableOptions;
    }

    public ConditionFactory(String id, BiPredicate<DataMap, R> condition) {
        this(id, condition, new ReadableOptions());
    }

    public static <A, T> ConditionFactory<Twin<A, T>> flippedFactory(DataTypeHolder<? extends Predicate<Twin<T, A>>> dataType) {
        return new ConditionFactory<>("flip_actor", (data, o) -> data.<Predicate<Twin<T, A>>>getUnsafe("condition").test(o.flipped()), new ReadableOptions().add("condition", dataType));
    }

    public static <A, T> ConditionFactory<Twin<A, T>> splitFactory(DataTypeHolder<? extends Predicate<A>> actorCondition, DataTypeHolder<? extends Predicate<T>> targetCondition) {
        return new ConditionFactory<>("split", (data, twin) -> {
            final Predicate<A> aC = data.getUnsafe("actor_condition", null);
            final Predicate<T> tC = data.getUnsafe("target_condition", null);
            return (aC == null || aC.test(twin.actor())) && (tC == null || tC.test(twin.target()));
        }, new ReadableOptions()
                .add("actor_condition", actorCondition, null)
                .add("target_condition", targetCondition, null)
        );
    }

    public static <T> ConditionFactory<T> andFactory(DataTypeHolder<? extends Predicate<T>> dataType) {
        return new ConditionFactory<>("and", (data, t) -> Threshold.forEachAND(data.<Collection<Predicate<T>>>getUnsafe("conditions"), condition -> condition.test(t)),
                new ReadableOptions().add("conditions", dataType.list()));
    }

    public static <T> ConditionFactory<T> orFactory(DataTypeHolder<? extends Predicate<T>> dataType) {
        return new ConditionFactory<>("or", (data, t) -> Threshold.forEachOR(data.<Collection<Predicate<T>>>getUnsafe("conditions"), condition -> condition.test(t)),
                new ReadableOptions().add("conditions", dataType.list()));
    }

    @Override
    public Predicate<R> build(DataMap data) {
        final Condition condition = new Condition(readableOptions.read(data));
        data.ifPresent("reverse", element -> condition.reverse = element.requireOf(DataPrimitive.class).requireValue(Boolean.class));
        return condition;
    }

    @Nonnull
    @Override
    public ReadableOptions getReadableOptions() {
        return readableOptions;
    }

    @Override
    public String id() {
        return id;
    }

    private class Condition implements Predicate<R> {
        private final DataMap data;
        private boolean reverse = false;

        public Condition(DataMap data) {
            this.data = data;
        }

        public boolean test(R r) {
            if (r == null) throw new NullPointerException("Data for condition was null");
            boolean test = ConditionFactory.this.condition.test(data, r);
            if (reverse) return !test;
            return test;
        }
    }
}
