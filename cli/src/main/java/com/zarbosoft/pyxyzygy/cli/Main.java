package com.zarbosoft.pyxyzygy.cli;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pidgooncommand.Command;
import io.github.classgraph.ClassGraph;

import java.util.ArrayList;
import java.util.List;

public class Main {
	@Configuration
	public abstract static class Subcommand {
		@Configuration
		@Command.Argument(description = "Path to a pyxyzygy project")
		public String path;
		public void run() {

		}
		public abstract void runImpl(ProjectContext projectContext);
	}

	@Configuration(name = "list")
	@Command.Argument(description = "Output project object structure and path element ids")
	public static class ListSubcommand extends Subcommand {

		@Override
		public void runImpl() {

		}
	}

	@Configuration(name = "export")
	@Command.Argument(description = "Export a project subtree as png, png offset relative to origin")
	public static class ExportSubcommand extends Subcommand {
		@Configuration
		public List<Integer> path = new ArrayList<>();
	}

	public static void main(String[] args) {
		Command.<Subcommand>parse(new ClassGraph()
				.enableAllInfo()
				.whitelistPackages("com.zarbosoft.pyxyzygy.cli")
				.scan(), Subcommand.class, args).runImpl();
	}
}
