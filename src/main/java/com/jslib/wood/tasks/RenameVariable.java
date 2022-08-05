package com.jslib.wood.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.docore.IFiles;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.ReturnCode;
import com.jslib.util.Files;
import com.jslib.util.TextReplace;
import com.jslib.wood.tasks.util.VariableReference;

public class RenameVariable extends WoodTask {
	private static final Log log = LogFactory.getLog(RenameVariable.class);

	private IFiles files;
	private TextReplace textReplace = new TextReplace();

	@Inject
	protected RenameVariable(IFiles files) {
		super();
		log.trace("RenameVariable(files)");
		this.files = files;
	}

	@Override
	public IParameters parameters() {
		log.trace("parameters()");
		IParameters parameters = super.parameters();
		parameters.define(0, "reference", VariableReference.class);
		parameters.define(1, "new-name", String.class);
		return parameters;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Exception {
		log.trace("execute(parameters)");

		Path projectDir = files.getProjectDir();
		VariableReference reference = parameters.get("reference", VariableReference.class);
		String newname = parameters.get("new-name");

		textReplace.setFilter(file -> isXML(file, reference.type()));
		textReplace.replaceAll(projectDir.toFile(), reference.name(), newname);

		textReplace.setFilter(file -> isTargetFile(file));
		String newreference = reference.clone(newname).value();
		textReplace.replaceAll(projectDir.toFile(), reference.value(), newreference);

		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "Rename WOOD project variable.";
	}

	@Override
	public String getDisplay() {
		return "Rename Variable";
	}

	private static boolean isXML(File file, String root) {
		try {
			return Files.isXML(file, root);
		} catch (IOException e) {
		}
		return false;
	}

	private static boolean isTargetFile(File file) {
		List<String> targetFileExtensions = Arrays.asList("htm", "css", "js", "xml", "json");
		for (String targetFileExtension : targetFileExtensions) {
			if (file.getName().endsWith(targetFileExtension)) {
				return true;
			}
		}
		return false;
	}
}
