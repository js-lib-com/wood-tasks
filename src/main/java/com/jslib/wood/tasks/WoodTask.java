package com.jslib.wood.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import com.jslib.docore.IFiles;
import com.jslib.dospi.AbstractTask;
import com.jslib.dospi.ITaskInfo;
import com.jslib.wood.tasks.util.TaskContext;

public abstract class WoodTask extends AbstractTask implements ITaskInfo {
	private static final String PROPERTIES_FILE = ".wood.properties";
	private static final String VERSION = "1.0";
	private static final LocalDate LAST_UPDATE = LocalDate.of(2022, 10, 15);
	private static final String AUTHOR = "Iulian Rotaru<mr.iulianrotaru@gmail.com>";

	protected final TaskContext context;

	protected WoodTask() {
		this.context = new TaskContext();
	}

	WoodTask(TaskContext context) {
		this.context = context;
	}

	@Override
	public boolean isExecutionContext() {
		Path workingDir = Paths.get("").toAbsolutePath();
		return Files.exists(workingDir.resolve(PROPERTIES_FILE));
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
		return VERSION;
	}

	@Override
	public LocalDate getLastUpdate() {
		return LAST_UPDATE;
	}

	@Override
	public String getAuthor() {
		return AUTHOR;
	}

	protected Path getProjectDir(IFiles files) {
		Path projectDir = files.getWorkingDir();
		Path propertiesFile = projectDir.resolve(PROPERTIES_FILE);
		if (!files.exists(propertiesFile)) {
			throw new IllegalStateException(String.format("Invalid WOOD project. Missing project properties file %s.", propertiesFile));
		}
		return projectDir;
	}
}
