package com.jslib.wood.tasks;

import java.io.IOException;
import java.nio.file.Path;

import javax.inject.Inject;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.docore.IFiles;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;
import com.jslib.util.Classes;
import com.jslib.util.Strings;
import com.jslib.wood.tasks.util.TaskContext;

public class CleanProject extends WoodTask {
	private static final Log log = LogFactory.getLog(CleanProject.class);

	private final IFiles files;

	@Inject
	public CleanProject(IFiles files) {
		super();
		log.trace("CleanProject(files)");
		this.files = files;
	}

	CleanProject(TaskContext context, IFiles files) {
		super(context);
		log.trace("CleanProject(files) - Test Constructor");
		this.files = files;
	}
	
	@Override
	public ReturnCode execute(IParameters parameters) throws IOException, TaskAbortException {
		log.trace("execute(parameters)");

		Path projectDir = getProjectDir(files);
		String projectName = files.getFileName(projectDir);
		log.info("Clean bulding files on project %s.", projectName);

		files.cleanDirectory(projectDir.resolve(context.getex("build.dir")));
		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "Clean build directory.";
	}

	@Override
	public String getDisplay() {
		return "Clean Project";
	}

	@Override
	public String help() throws IOException {
		log.trace("hep()");
		return Strings.load(Classes.getResourceAsReader("manual/clean-project.md"));
	}
}
