package io.github.thegatesdev.actionable.builder;

import io.github.thegatesdev.actionable.registry.DataBuilder;
import io.github.thegatesdev.maple.data.DataMap;
import io.github.thegatesdev.maple.data.DataValue;
import io.github.thegatesdev.maple.data.Keyed;
import io.github.thegatesdev.maple.read.ReadableOptions;
import io.github.thegatesdev.maple.read.struct.DataTypeHolder;
import io.github.thegatesdev.threshold.util.twin.Twin;

import javax.annotation.Nonnull;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ConditionBuilder<R> implements DataBuilder<Predicate<R>>, Keyed {
    private final String key;
    private final BiPredicate<DataMap, R> predicate;
    private final ReadableOptions readableOptions;

    public ConditionBuilder(String key, BiPredicate<DataMap, R> predicate, ReadableOptions readableOptions) {
        this.key = key;
        this.predicate = predicate;
        this.readableOptions = readableOptions;
    }

    public ConditionBuilder(String key, BiPredicate<DataMap, R> predicate) {
        this(key, predicate, new ReadableOptions());
    }

    public static <A, T> ConditionBuilder<Twin<A, T>> flippedFactory(DataTypeHolder<DataValue<Predicate<Twin<T, A>>>> dataType) {
        return new ConditionBuilder<>("flip", (data, o) -> data.<Predicate<Twin<T, A>>>getUnsafe("condition").test(o.flipped()), new ReadableOptions().add("condition", dataType));
    }

    public static <A, T> ConditionBuilder<Twin<A, T>> splitAndFactory(DataTypeHolder<DataValue<Predicate<A>>> actorCondition, DataTypeHolder<DataValue<Predicate<T>>> targetCondition) {
        return new ConditionBuilder<>("split_all", (data, twin) -> {
            Predicate<A> actorC = data.getUnsafe("actor_condition", null);
            if (actorC != null && !actorC.test(twin.actor())) return false;
            Predicate<T> targetC = data.getUnsafe("target_condition", null);
            return targetC != null && targetC.test(twin.target());
        }, new ReadableOptions()
            .addOptional("actor_condition", actorCondition)
            .addOptional("target_condition", targetCondition)
        );
    }

    public static <A, T> ConditionBuilder<Twin<A, T>> splitOrFactory(DataTypeHolder<DataValue<Predicate<A>>> actorCondition, DataTypeHolder<DataValue<Predicate<T>>> targetCondition) {
        return new ConditionBuilder<>("split_any", (data, twin) -> {
            Predicate<A> actorC = data.getUnsafe("actor_condition", null);
            if (actorC != null && actorC.test(twin.actor())) return true;
            Predicate<T> targetC = data.getUnsafe("target_condition", null);
            return targetC != null && targetC.test(twin.target());
        }, new ReadableOptions()
            .addOptional("actor_condition", actorCondition)
            .addOptional("target_condition", targetCondition)
        );
    }

    public static <T> ConditionBuilder<T> andFactory(DataTypeHolder<DataValue<Predicate<T>>> dataType) {
        return new ConditionBuilder<>("all_true", (data, t) -> {
            var list = data.getList("conditions");
            for (int i = 0; i < list.size(); i++) {
                if (!list.<Predicate<T>>getUnsafe(i).test(t)) return false;
            }
            return true;
        }, new ReadableOptions().add("conditions", dataType.dataType().list()));
    }

    public static <T> ConditionBuilder<T> orFactory(DataTypeHolder<DataValue<Predicate<T>>> dataType) {
        return new ConditionBuilder<>("any_true", (data, t) -> {
            var list = data.getList("conditions");
            for (int i = 0; i < list.size(); i++) {
                if (list.<Predicate<T>>getUnsafe(i).test(t)) return true;
            }
            return false;
        }, new ReadableOptions().add("conditions", dataType.dataType().list()));
    }

    @Override
    public Predicate<R> build(DataMap data) {
        return new Condition<>(this.predicate, readableOptions.read(data), data.getBoolean("reverse", false));
    }

    @Nonnull
    public ReadableOptions readableOptions() {
        return readableOptions;
    }

    @Override
    public String key() {
        return key;
    }

    private record Condition<R>(BiPredicate<DataMap, R> condition, DataMap data,
                                boolean reverse) implements Predicate<R> {
        @Override
        public boolean test(final R r) {
            return condition.test(data, r) != reverse;
        }
    }
}
