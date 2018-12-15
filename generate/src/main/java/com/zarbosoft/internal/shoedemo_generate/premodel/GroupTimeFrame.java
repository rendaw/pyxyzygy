package com.zarbosoft.internal.shoedemo_generate.premodel;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.shoedemo.model.ProjectObject;

@Configuration
public class GroupTimeFrame extends ProjectObject {
	@Configuration
	public int offset;
	@Configuration
	public int length;
	@Configuration
	public int innerOffset;
	@Configuration
	public int innerLength;
}
