package com.zarbosoft.shoedemo.modelhelp;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Model {
	boolean noVersion() default false;
	boolean readOnly() default false;
}
