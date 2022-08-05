package com.jslib.wood.tasks;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.docore.IFiles;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.IShell;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;
import com.jslib.dospi.UserCancelException;

public class DeleteComponent extends WoodTask {
	private static final Log log = LogFactory.getLog(DeleteComponent.class);

	private final IShell shell;
	private final IFiles files;

	@Inject
	public DeleteComponent(IShell shell, IFiles files) {
		super();
		log.trace("DeleteComponent(shell, files)");
		this.shell = shell;
		this.files = files;
	}

	@Override
	public IParameters parameters() {
		log.trace("parameters()");
		IParameters parameters = super.parameters();
		parameters.define(0, "name", String.class);
		return parameters;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws IOException, UserCancelException, TaskAbortException {
		log.trace("execute(parameters)");

		String name = parameters.get("name", String.class);

		Path projectDir = files.getProjectDir();
		Path compoDir = projectDir.resolve(name);
		if (!files.exists(compoDir)) {
			throw new TaskAbortException("Missing component directory %s.", compoDir);
		}

		List<String> usedByPaths = files.findFilesByContentPattern(projectDir, ".htm", name).stream().map(file -> projectDir.relativize(file).toString().replace('\\', '/')).filter(path -> !path.startsWith(name)).collect(Collectors.toList());
		if (!usedByPaths.isEmpty()) {
			log.warn("Component %s is used by:", name);
			for (String usedByPath : usedByPaths) {
				log.warn("- %s", usedByPath);
			}
			return ReturnCode.ABORT;
		}

		shell.getConsole().confirm("All component '%s' files will be permanently deleted", name);
		files.cleanDirectory(compoDir);
		files.delete(compoDir);
		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "Delete component and all constituent files.";
	}

	@Override
	public String getDisplay() {
		return "Delete Component";
	}
}
