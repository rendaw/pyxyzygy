package com.zarbosoft.shoedemo.model;

import com.zarbosoft.interface1.Configuration;
import org.apache.tools.ant.Project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

	public ChangeStep apply(ProjectBase project, long id) {
		this.id = id;
		ChangeStep out = new ChangeStep(project.nextId++);
		for (Change change : changes) change.apply(out);
		return out;
	}

	public static Path path(Project project, int id) {
		return project.tileDir.resolve(String.format("%s.luxem", id));
	}

	public static ChangeStep fromId(Project project, int id) {
		return new ChangeStep(id, path(project, id).toString());
	}

	public void delete(Project project) {
		changes.forEach(change -> change.delete(project));
		Files.delete(path(project, id));
	}
}
