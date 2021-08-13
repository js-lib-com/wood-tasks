package com.jslib.wood.tasks;

import static java.lang.String.format;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import javax.inject.Inject;

import com.jslib.docore.IFiles;
import com.jslib.dospi.Flags;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;
import com.jslib.wood.tasks.util.CompoUtils;

import js.log.Log;
import js.log.LogFactory;

public class OpenPage extends WoodTask {
	private static final Log log = LogFactory.getLog(OpenPage.class);

	private final IFiles files;
	private final CompoUtils compos;
	private final Desktop desktop;

	@Inject
	public OpenPage(IFiles files, CompoUtils compos) {
		super();
		log.trace("OpenPage(files, compos)");
		this.files = files;
		this.compos = compos;
		this.desktop = Desktop.getDesktop();
	}

	@Override
	public IParameters parameters() {
		log.trace("parameters()");
		IParameters parameters = super.parameters();
		parameters.define(0, "page-name", String.class);
		parameters.define(1, "target", Flags.ARGUMENT, Target.class, Target.local);
		return parameters;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws IOException, TaskAbortException {
		log.trace("execute()");

		Path projectDir = files.getProjectDir();
		String projectName = files.getFileName(projectDir);

		String pageName = parameters.get("page-name", String.class);
		Path compoDir = pageName.contains("/") ? projectDir.resolve(pageName) : compos.findCompoByPageName(pageName);
		pageName = files.getFileName(compoDir);

		String pageURI = null;
		switch (parameters.get("target", Target.class)) {
		case local:
			int port = context.getex("runtime.port", int.class);
			String appName = context.getex("runtime.context", projectName);
			pageURI = format("http://localhost:%d/%s/%s.htm", port, appName, pageName);
			break;

		case server:
			String server = context.getex("dev.server");
			if (!server.endsWith("/")) {
				server += '/';
			}
			pageURI = format("https://%s%s/%s.htm", server, projectName, pageName);
			break;
		}
		log.info("Open page %s.", pageURI);

		desktop.browse(URI.create(pageURI));
		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "Open requested page on browser.";
	}

	@Override
	public String getDisplay() {
		return "Open Page";
	}

	private enum Target {
		local, server
	}
}
