package io.github.thegatesdev.actionable.registry;

import io.github.thegatesdev.maple.data.DataMap;
import io.github.thegatesdev.maple.read.struct.ReadableOptionsHolder;

public interface DataBuilder<Data> extends ReadableOptionsHolder {

    Data build(DataMap data);
}
