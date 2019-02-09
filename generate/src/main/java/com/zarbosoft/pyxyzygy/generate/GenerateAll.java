package com.zarbosoft.pyxyzygy.generate;

import com.zarbosoft.pyxyzygy.generate.model.v0.GenerateModelV0;

public class GenerateAll extends TaskBase{
	public static void main(final String[] args) {
		final GenerateAll t = new GenerateAll();
		t.setPath(args[0]);
		t.execute();
	}

	@Override
	public void run() {
		new GenerateModelV0(){
			{
				setPath(path.toString());
			}
		}.run();
		new GenerateGraphicsProxy(){
			{
				setPath(path.toString());
			}
		}.run();
	}
}
