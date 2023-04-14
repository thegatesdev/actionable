package io.github.thegatesdev.actionable.factory;

import io.github.thegatesdev.actionable.util.twin.Twin;
import io.github.thegatesdev.maple.data.DataMap;
import io.github.thegatesdev.mapletree.data.DataTypeHolder;
import io.github.thegatesdev.mapletree.data.Factory;
import io.github.thegatesdev.mapletree.data.ReadableOptions;
import io.github.thegatesdev.mapletree.data.ReadableOptionsHolder;
import io.github.thegatesdev.mapletree.registry.Identifiable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ConditionFactory<R> implements Identifiable, Factory<Predicate<R>>, ReadableOptionsHolder {
    private final String id;
    private final BiPredicate<DataMap, R> predicate;
    private final ReadableOptions readableOptions;

    public ConditionFactory(String id, BiPredicate<DataMap, R> predicate, ReadableOptions readableOptions) {
        this.id = id;
        this.predicate = predicate;
        this.readableOptions = readableOptions;
    }

    public ConditionFactory(String id, BiPredicate<DataMap, R> predicate) {
        this(id, predicate, new ReadableOptions());
    }

    public static <A, T> ConditionFactory<Twin<A, T>> flippedFactory(DataTypeHolder<? extends Predicate<Twin<T, A>>> dataType) {
        return new ConditionFactory<>("flip", (data, o) -> data.<Predicate<Twin<T, A>>>getUnsafe("condition").test(o.flipped()), new ReadableOptions().add("condition", dataType));
    }

    public static <A, T> ConditionFactory<Twin<A, T>> splitAndFactory(DataTypeHolder<? extends Predicate<A>> actorCondition, DataTypeHolder<? extends Predicate<T>> targetCondition) {
        return new ConditionFactory<>("split_and", (data, twin) -> {
            final Predicate<A> aC = data.getUnsafe("actor_condition", null);
            final Predicate<T> tC = data.getUnsafe("target_condition", null);
            return (aC == null || aC.test(twin.actor())) && (tC == null || tC.test(twin.target()));
        }, new ReadableOptions()
                .add("actor_condition", actorCondition, null)
                .add("target_condition", targetCondition, null)
        );
    }

    public static <A, T> ConditionFactory<Twin<A, T>> splitOrFactory(DataTypeHolder<? extends Predicate<A>> actorCondition, DataTypeHolder<? extends Predicate<T>> targetCondition) {
        return new ConditionFactory<>("split_or", (data, twin) -> {
            final Predicate<A> aC = data.getUnsafe("actor_condition", null);
            final Predicate<T> tC = data.getUnsafe("target_condition", null);
            return (aC == null || aC.test(twin.actor())) || (tC == null || tC.test(twin.target()));
        }, new ReadableOptions()
                .add("actor_condition", actorCondition, null)
                .add("target_condition", targetCondition, null)
        );
    }

    public static <T> ConditionFactory<T> andFactory(DataTypeHolder<? extends Predicate<T>> dataType) {
        return new ConditionFactory<>("and", (data, t) -> {
            final var conditions = data.<List<Predicate<T>>>getUnsafe("conditions");
            for (int i = 0, unsafeSize = conditions.size(); i < unsafeSize; i++)
                if (!conditions.get(i).test(t)) return false;
            return true;
        }, new ReadableOptions().add("conditions", dataType.list()));
    }

    public static <T> ConditionFactory<T> orFactory(DataTypeHolder<? extends Predicate<T>> dataType) {
        return new ConditionFactory<>("or", (data, t) -> {
            final var conditions = data.<List<Predicate<T>>>getUnsafe("conditions");
            for (int i = 0, conditionsSize = conditions.size(); i < conditionsSize; i++)
                if (conditions.get(i).test(t)) return true;
            return false;
        }, new ReadableOptions().add("conditions", dataType.list()));
    }

    @Override
    public Predicate<R> build(DataMap data) {
        return new Condition<>(this.predicate, readableOptions.read(data), data.getBoolean("reverse", false));
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

    private record Condition<R>(BiPredicate<DataMap, R> condition, DataMap data,
                                boolean reverse) implements Predicate<R> {
        @Override
        public boolean test(final R r) {
            return condition.test(data, r) != reverse;
        }
    }
}
