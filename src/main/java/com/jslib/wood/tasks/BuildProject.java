package com.jslib.wood.tasks;

import java.nio.file.Path;

import javax.inject.Inject;

import com.jslib.docore.IFiles;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;

import js.log.Log;
import js.log.LogFactory;
import js.wood.build.Builder;
import js.wood.build.BuilderConfig;

public class BuildProject extends WoodTask {
	private static final Log log = LogFactory.getLog(BuildProject.class);

	private final IFiles files;
	private int buildNumber;

	@Inject
	public BuildProject(IFiles files) {
		super();
		log.trace("BuildProject(files)");
		this.files = files;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Exception {
		log.trace("execute(parameters)");

		Path projectDir = files.getProjectDir();
		Path buildDir = projectDir.resolve(context.getex("build.dir"));
		if (!files.exists(buildDir)) {
			throw new TaskAbortException("Missing build directory %s.", buildDir);
		}

		BuilderConfig builderConfig = new BuilderConfig();
		builderConfig.setProjectDir(projectDir.toFile());
		builderConfig.setBuildNumber(buildNumber);

		String projectName = files.getFileName(projectDir);
		log.info("Build project %s.", projectName);
		Builder builder = builderConfig.createBuilder();
		builder.build();

		String runtimeHome = context.getex("runtime.home");
		String runtimeName = context.getex("runtime.name", projectName);
		String contextName = context.getex("runtime.context", projectName);
		Path deployDir = files.createDirectories(runtimeHome, runtimeName, "webapps", contextName);

		files.copyFiles(buildDir, deployDir);
		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "Build WOOD project.";
	}

	@Override
	public String getDisplay() {
		return "Build Project";
	}
}
