package com.jslib.wood.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import com.jslib.dospi.AbstractTask;
import com.jslib.dospi.ITaskInfo;
import com.jslib.wood.tasks.util.TaskContext;

public abstract class WoodTask extends AbstractTask implements ITaskInfo {
	private static final String PROJECT_DESCRIPTOR_FILE = "project.xml";
	private static final LocalDate LAST_UPDATE = LocalDate.of(2021, 7, 20);

	protected final TaskContext context = new TaskContext();

	@Override
	public boolean isExecutionContext() {
		Path workingDir = Paths.get("").toAbsolutePath();
		return Files.exists(workingDir.resolve(PROJECT_DESCRIPTOR_FILE));
	}

	@Override
	public ITaskInfo getInfo() {
		return this;
	}

	@Override
	public String help() throws IOException {
		return String.format("# %s help not implemented", getClass().getCanonicalName());
	}

	@Override
	public String getVersion() {
		return "0.0.1-SNAPSHOT";
	}

	@Override
	public LocalDate getLastUpdate() {
		return LAST_UPDATE;
	}

	@Override
	public String getAuthor() {
		return "Iulian Rotaru";
	}
}
