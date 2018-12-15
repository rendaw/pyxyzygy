package com.zarbosoft.shoedemo.deserialize;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.rendaw.common.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.zarbosoft.rendaw.common.Common.last;

public class GeneralStateBuilder {
	@FunctionalInterface
	interface Converter {
		Object convert(String type, Object value);
	}

	private final List<Pair<Function<Converter, StackReader.State>, Converter>>
			states = new ArrayList<>();

	public GeneralStateBuilder list() {
		final int at = states.size();
		states.add(new Pair<>(convert -> {
			return new GeneralListState(this, at, convert);
		}, null));
		return this;
	}

	public GeneralStateBuilder idList() {
		states.add(new Pair<>(_i -> new IDListState(), null));
		return this;
	}

	public GeneralStateBuilder set() {
		final int at = states.size();
		states.add(new Pair<>(convert -> {
			return new GeneralSetState(this, at, convert);
		}, null));
		return this;
	}

	public GeneralStateBuilder idSet() {
		states.add(new Pair<>(_i -> new IDSetState(), null));
		return this;
	}

	public GeneralStateBuilder map() {
		final int at = states.size();
		states.add(new Pair<>(convert -> {
			return new GeneralMapState(this, at, convert);
		}, null));
		return this;
	}

	public GeneralStateBuilder idMap() {
		states.add(new Pair<>(_i -> new IDMapState(), null));
		return this;
	}

	public GeneralStateBuilder convert(Converter convert) {
		last(states).second = convert;
		return this;
	}

	public StackReader.State next(int at) {
		Pair<Function<Converter, StackReader.State>, Converter> pair =
				states.get(at + 1);
		return pair.first.apply(pair.second == null ? (t, v) -> v : pair.second);
	}

	public StackReader.State build() {
		return next(-1);
	}
}
