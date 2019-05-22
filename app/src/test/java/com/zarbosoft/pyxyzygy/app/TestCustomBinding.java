package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.app.widgets.binding.CustomBinding;
import com.zarbosoft.pyxyzygy.app.widgets.binding.PropertyBinder;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class TestCustomBinding {

	@Test
	public void testBindBidirectional() {
		SimpleObjectProperty<Integer> a = new SimpleObjectProperty<>(15);
		SimpleObjectProperty<Integer> b = new SimpleObjectProperty<>(0);
		CustomBinding.bindBidirectional(new PropertyBinder<>(a), new PropertyBinder<>(b));
		assertThat(a.get(), equalTo(15));
		assertThat(b.get(), equalTo(15));
	}
}
