package com.jslib.wood.tasks;

import static java.lang.String.format;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;

import javax.inject.Inject;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.docore.IFiles;
import com.jslib.docore.Velocity;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;
import com.jslib.wood.preview.EventsServlet;
import com.jslib.wood.preview.FileSystemWatcher;
import com.jslib.wood.preview.ForwardFilter;
import com.jslib.wood.preview.PreviewServlet;
import com.jslib.wood.tasks.util.CompoUtils;

public class PreviewPage extends WoodTask {
	private static final Log log = LogFactory.getLog(PreviewPage.class);

	private final IFiles files;
	private final CompoUtils compos;
	private final Desktop desktop;

	@Inject
	public PreviewPage(IFiles files, CompoUtils compos) {
		super();
		log.trace("PreviewPage()");
		this.files = files;
		this.compos = compos;
		this.desktop = Desktop.getDesktop();
	}

	@Override
	public IParameters parameters() {
		log.trace("parameters()");
		IParameters parameters = super.parameters();
		parameters.define(0, "page-name", String.class);
		return parameters;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Exception {
		log.trace("execute()");

		Path projectDir = files.getProjectDir();

		String pageName = parameters.get("page-name", String.class);
		Path compoDir = pageName.contains("/") ? projectDir.relativize(projectDir.resolve(pageName)) : compos.findCompoByPageName(pageName);
		if (!files.exists(compoDir)) {
			throw new TaskAbortException("Missing component directory %s.", compoDir);
		}
		String pagePath = compoDir.toString().replace('\\', '/');

		String projectName = files.getFileName(projectDir);
		String runtimeHome = context.getex("runtime.home");
		String runtimeName = context.getex("runtime.name", projectName);
		String contextName = context.getex("runtime.context", projectName) + "-preview";
		Path deployDir = files.createDirectories(runtimeHome, runtimeName, "webapps", contextName);

		Path webxmlFile = deployDir.resolve("WEB-INF/web.xml");
		if (!files.exists(webxmlFile)) {
			files.createDirectories(webxmlFile.getParent());

			Velocity template = new Velocity("WEB-INF/preview-web.vtl");
			template.put("FileSystemWatcher", FileSystemWatcher.class.getCanonicalName());
			template.put("ForwardFilter", ForwardFilter.class.getCanonicalName());
			template.put("PreviewServlet", PreviewServlet.class.getCanonicalName());
			template.put("EventsServlet", EventsServlet.class.getCanonicalName());
			template.put("display", context.get("project.display", projectName));
			template.put("description", context.get("project.description", projectName));
			template.put("projectDir", projectDir.toAbsolutePath().toString());
			template.put("buildDir", context.get("build.dir"));
			template.writeTo(files.getWriter(webxmlFile));

			log.info("Created missing preview configuration.");
			log.info("Please allow a moment for runtime to updates.");
			return ReturnCode.ABORT;
		}

		int port = context.getex("runtime.port", int.class);
		String pageURI = format("http://localhost:%d/%s/%s", port, contextName, pagePath);
		log.info("Preview page %s.", pageURI);

		desktop.browse(URI.create(pageURI));
		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "Preview requested page on browser.";
	}

	@Override
	public String getDisplay() {
		return "Preview Page";
	}
}
