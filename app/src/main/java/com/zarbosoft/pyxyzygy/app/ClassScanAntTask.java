package com.zarbosoft.pyxyzygy.app;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClassScanAntTask extends Task {
	public String path;

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public void execute() throws BuildException {
		Path path = Paths.get(this.path);
		try {
			Files.createDirectories(path.getParent());
			try (
					ScanResult scanResult = new ClassGraph()
							.whitelistPackages("com.zarbosoft.pyxyzygy.app")
							.enableAllInfo()
							.scan(); OutputStream out = Files.newOutputStream(path)
			) {
				if (scanResult.getAllClasses().isEmpty())
					throw new RuntimeException("No classes found!");
				out.write(scanResult.toJSON(4).getBytes(StandardCharsets.UTF_8));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
