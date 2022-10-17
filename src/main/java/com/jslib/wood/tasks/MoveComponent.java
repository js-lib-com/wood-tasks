package com.jslib.wood.tasks;

import static java.lang.String.format;
import static com.jslib.util.Strings.concat;

import java.nio.file.Path;

import javax.inject.Inject;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.docore.IFiles;
import com.jslib.dospi.IConsole;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.IShell;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;
import com.jslib.util.TextReplace;

public class MoveComponent extends WoodTask {
	private static final Log log = LogFactory.getLog(MoveComponent.class);

	private final IShell shell;
	private final IFiles files;
	private final TextReplace textReplace;

	@Inject
	public MoveComponent(IShell shell, IFiles files) {
		super();
		log.trace("MoveComponent(shell, files)");
		this.shell = shell;
		this.files = files;
		this.textReplace = new TextReplace();
	}

	@Override
	public IParameters parameters() {
		log.trace("parameters()");
		IParameters parameters = super.parameters();
		parameters.define(0, "component-path", String.class);
		parameters.define(1, "target-path", String.class);
		return parameters;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Exception {
		log.trace("execute(parameters)");

		String compoPath = parameters.get("component-path");
		String targetPath = parameters.get("target-path");

		Path projectDir = getProjectDir(files);
		Path compoDir = projectDir.resolve(compoPath);
		if (!files.exists(compoDir)) {
			throw new TaskAbortException("Missing component directory %s.", compoDir);
		}
		String compoName = files.getFileName(compoDir);

		IConsole console = shell.getConsole();
		Path targetDir = projectDir.resolve(targetPath);
		if (!files.exists(targetDir)) {
			console.confirm("Create missing %s directory", targetDir);
			files.createDirectory(targetDir);
		}

		Path targetCompoDir = targetDir.resolve(compoName);
		if (files.exists(targetCompoDir)) {
			throw new TaskAbortException("Existing target component %s.", targetCompoDir);
		}
		files.createDirectory(targetCompoDir);

		log.info("Move %s component to %s.", compoPath, targetPath);
		for (Path compoFile : files.listFiles(compoDir)) {
			Path targetCompoFile = targetCompoDir.resolve(files.getFileName(compoFile));
			log.info("Move %s file to %s.", compoFile, targetCompoFile);
			files.move(compoFile, targetCompoFile);
		}
		files.delete(compoDir);

		String targetCompoPath = format("%s/%s", targetPath, compoName);
		log.info("Replace %s with %s in .htm files.", compoPath, targetCompoPath);
		textReplace.addExclude("");
		textReplace.setFileExtension("htm");
		textReplace.replaceAll(projectDir.toFile(), compoPath, targetCompoPath);

		String compoScript = concat(compoPath, '/', compoName, ".js");
		String targetCompoScript = concat(targetPath, '/', compoName, '/', compoName, ".js");
		Path targetCompoScriptFile = projectDir.resolve(targetCompoScript);
		log.info("Replace %s with %s in .xml files.", compoScript, targetCompoScript);
		if (files.exists(targetCompoScriptFile)) {
			textReplace.setFileExtension("xml");
			textReplace.replaceAll(projectDir.toFile(), compoScript, targetCompoScript);
		}

		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "Move component to different path.";
	}

	@Override
	public String getDisplay() {
		return "Move Component";
	}
}
