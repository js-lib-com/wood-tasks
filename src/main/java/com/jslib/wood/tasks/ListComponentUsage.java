package com.jslib.wood.tasks;

import java.nio.file.Path;
import java.util.List;

import javax.inject.Inject;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.docore.IFiles;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.IPrintout;
import com.jslib.dospi.IShell;
import com.jslib.dospi.ReturnCode;

public class ListComponentUsage extends WoodTask {
	private static final Log log = LogFactory.getLog(ListComponentUsage.class);

	private final IShell shell;
	private final IFiles files;

	@Inject
	public ListComponentUsage(IShell shell, IFiles files) {
		super();
		log.trace("ListComponentUsage(shell, files)");
		this.shell = shell;
		this.files = files;
	}

	@Override
	public IParameters parameters() {
		log.trace("parameters()");
		IParameters parameters = super.parameters();
		parameters.define(0, "component-path", String.class);
		return parameters;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Exception {
		log.trace("execute(parameters)");

		String compoPath = parameters.get("component-path");
		log.info("List component usage for %s.", compoPath);

		Path projectDir = getProjectDir(files);

		List<Path> usedByFiles = files.findFilesByContentPattern(projectDir, ".htm", compoPath);
		if (usedByFiles.isEmpty()) {
			log.info("Component %s is not used.", compoPath);
			return ReturnCode.SUCCESS;
		}

		IPrintout printout = shell.getPrintout();
		printout.addHeading1(String.format("Component %s is used by:", compoPath));
		printout.createUnorderedList();
		for (Path usedByFile : usedByFiles) {
			printout.addListItem(projectDir.relativize(usedByFile).toString());
		}
		printout.display();

		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "List files where component is used.";
	}

	@Override
	public String getDisplay() {
		return "List Component Usage";
	}
}
