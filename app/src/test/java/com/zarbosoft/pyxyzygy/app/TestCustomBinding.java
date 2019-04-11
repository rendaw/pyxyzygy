package com.zarbosoft.pyxyzygy.app;

import javafx.beans.property.SimpleObjectProperty;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class TestCustomBinding {

	@Test
	public void testBindBidirectional() {
		SimpleObjectProperty<Integer> a = new SimpleObjectProperty<>(15);
		SimpleObjectProperty<Integer> b = new SimpleObjectProperty<>(0);
		CustomBinding.bindBidirectional(new CustomBinding.PropertyBinder<>(a), new CustomBinding.PropertyBinder<>(b));
		assertThat(a.get(), equalTo(15));
		assertThat(b.get(), equalTo(15));
	}
}
