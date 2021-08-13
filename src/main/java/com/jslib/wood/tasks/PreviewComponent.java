package com.jslib.wood.tasks;

import static java.lang.String.format;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;

import javax.inject.Inject;

import com.jslib.docore.IFiles;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;

import js.log.Log;
import js.log.LogFactory;

public class PreviewComponent extends WoodTask {
	private static final Log log = LogFactory.getLog(PreviewComponent.class);

	private final IFiles files;
	private final Desktop desktop;

	@Inject
	public PreviewComponent(IFiles files) {
		super();
		log.trace("PreviewComponent(files)");
		this.files = files;
		this.desktop = Desktop.getDesktop();
	}

	@Override
	public IParameters parameters() {
		log.trace("parameters()");
		IParameters parameters = super.parameters();
		parameters.define(0, "compo-name", String.class);
		return parameters;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Exception {
		log.trace("execute()");

		String compoName = parameters.get("compo-name", String.class);

		Path projectDir = files.getProjectDir();
		Path compoDir = projectDir.resolve(compoName);
		if (!files.exists(compoDir)) {
			throw new TaskAbortException("Missing component directory %s.", compoDir);
		}

		String projectName = files.getFileName(projectDir);
		String contextName = context.getex("runtime.context", projectName) + "-preview";

		int port = context.getex("runtime.port", int.class);
		String compoURI = format("http://localhost:%d/%s/%s", port, contextName, compoName);
		log.info("Preview component %s.", compoURI);

		desktop.browse(URI.create(compoURI));
		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "Preview requested component on browser.";
	}

	@Override
	public String getDisplay() {
		return "Preview Component";
	}
}
