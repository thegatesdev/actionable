package io.github.thegatesdev.actionable.registry;

import io.github.thegatesdev.maple.data.DataMap;
import io.github.thegatesdev.maple.read.Options;

public interface DataBuilder<Data> {

    Data build(DataMap data);

    Options options();
}
