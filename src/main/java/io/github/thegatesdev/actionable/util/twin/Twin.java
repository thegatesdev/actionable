package io.github.thegatesdev.actionable.util.twin;

public interface Twin<A, T> {
    A actor();

    T target();

    Twin<T, A> flipped();

    static <A, T> Twin<A, T> of(A actor, T target) {
        return new ImmutableTwin<>(actor, target);
    }

    default boolean areEqual() {
        return target().equals(actor());
    }
}
