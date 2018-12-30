package com.zarbosoft.shoedemo.model;

import com.zarbosoft.interface1.Configuration;
import org.apache.tools.ant.Project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.zarbosoft.rendaw.common.Common.reversed;

@Configuration
public class ChangeStep {
	public long id;
	public List<Change> changes = new ArrayList<>();

	public ChangeStep(long id) {
		this.id = id;
	}

	public void add(Change change) {
		changes.add(change);
	}

	public ChangeStep apply(ProjectContextBase project, long id) {
		this.id = id;
		ChangeStep out = new ChangeStep(project.nextId++);
		for (Change change : reversed(changes)) change.apply(project, out);
		return out;
	}

	public void remove(ProjectContextBase context) {
		for (Change c : changes) c.delete(context);
	}
}
