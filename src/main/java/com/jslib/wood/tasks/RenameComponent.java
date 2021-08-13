package com.jslib.wood.tasks;

import static js.util.Strings.concat;

import java.io.IOException;
import java.nio.file.Path;

import javax.inject.Inject;

import com.jslib.docore.IFiles;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;

import js.log.Log;
import js.log.LogFactory;
import js.util.TextReplace;

public class RenameComponent extends WoodTask {
	private static final Log log = LogFactory.getLog(RenameComponent.class);

	private final IFiles files;
	private final Path projectDir;
	private final TextReplace textReplace;

	@Inject
	public RenameComponent(IFiles files) {
		super();
		log.trace("RenameComponent()");
		this.files = files;
		this.projectDir = files.getProjectDir();
		this.textReplace = new TextReplace();
	}

	@Override
	public IParameters parameters() {
		log.trace("parameters()");
		IParameters parameters = super.parameters();
		parameters.define(0, "compo-name", String.class);
		parameters.define(1, "new-name", String.class);
		return parameters;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws IOException, TaskAbortException {
		log.trace("execute(parameters)");
		String name = parameters.get("compo-name", String.class);
		String newname = parameters.get("new-name");

		Path compoDir = projectDir.resolve(name);
		if (!files.exists(compoDir)) {
			throw new TaskAbortException("Missing component directory %s.", compoDir);
		}

		Path descriptorFile = compoDir.resolve(files.getFileName(compoDir) + ".xml");
		if (!files.exists(descriptorFile)) {
			throw new TaskAbortException("Path %s is not a component.", name);
		}

		Path newCompoDir = compoDir.getParent().resolve(newname);
		if (files.exists(newCompoDir)) {
			throw new TaskAbortException("Target component directory %s already exist.", newCompoDir);
		}

		// rename component files into current component directory then rename directory too
		for (Path compoFile : files.listFiles(compoDir, path -> files.getFileBasename(path).equals(files.getFileName(compoDir)))) {
			Path newCompoFile = compoDir.resolve(concat(newname, '.', files.getExtension(compoFile)));
			log.info("Rename %s file to %s.", compoFile, newCompoFile);
			files.move(compoFile, newCompoFile);
		}
		files.move(compoDir, newCompoDir);

		String compoPath = name;
		int pathSeparator = compoPath.lastIndexOf('/') + 1;
		String newCompoPath;
		if (pathSeparator > 0) {
			newCompoPath = compoPath.substring(0, pathSeparator) + newname;
		} else {
			newCompoPath = newname;
		}

		textReplace.addExclude("");
		textReplace.setFileExtension("htm");
		textReplace.replaceAll(projectDir.toFile(), compoPath, newCompoPath);

		String compoScript = concat(compoPath, '/', files.getFileName(compoDir), ".js");
		String newCompoScript = concat(newCompoPath, '/', files.getFileName(newCompoDir), ".js");
		Path newCompoScriptFile = projectDir.resolve(newCompoScript);
		if (files.exists(newCompoScriptFile)) {
			textReplace.setFileExtension("xml");
			textReplace.replaceAll(projectDir.toFile(), compoScript, newCompoScript);
		}
		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "Rename existing component.";
	}

	@Override
	public String getDisplay() {
		return "Rename Component";
	}
}
