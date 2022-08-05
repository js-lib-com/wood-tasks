package com.jslib.wood.tasks;

import java.nio.file.Path;

import javax.inject.Inject;

import com.jslib.api.dom.Document;
import com.jslib.api.dom.DocumentBuilder;
import com.jslib.api.dom.Element;
import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.docore.IFiles;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;
import com.jslib.util.Classes;

public class BindRuntime extends WoodTask {
	private static final Log log = LogFactory.getLog(BindRuntime.class);

	private final IFiles files;

	@Inject
	public BindRuntime(IFiles files) {
		super();
		log.trace("BindRuntime(files)");
		this.files = files;
	}

	@Override
	public IParameters parameters() {
		log.trace("parameters()");
		IParameters parameters = super.parameters();
		parameters.define(0, "runtime-name", String.class);
		return parameters;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Exception {
		log.trace("execute(parameters)");

		String runtimeName = parameters.get("runtime-name");

		Path runtimesHomeDir = files.getPath(context.getex("runtime.home"));
		Path projectRuntimeDir = runtimesHomeDir.resolve(runtimeName);
		if (!files.exists(projectRuntimeDir)) {
			throw new TaskAbortException("Missing runtime directory %s.", projectRuntimeDir);
		}

		Path serverXmlFile = projectRuntimeDir.resolve("conf/server.xml");
		if (!files.exists(projectRuntimeDir)) {
			throw new TaskAbortException("Invalid runtime %s. Missing server.xml file.", runtimeName);
		}

		DocumentBuilder docBuilder = Classes.loadService(DocumentBuilder.class);
		Document serverXmlDoc = docBuilder.loadXML(files.getInputStream(serverXmlFile));
		Element connectorElement = serverXmlDoc.getByXPath("//Connector[contains(@protocol,'HTTP')]");
		if (connectorElement == null) {
			throw new TaskAbortException("Invalid runtime %s. Missing connector from server.xml file.", runtimeName);
		}

		String connectorPort = connectorElement.getAttr("port");
		if (connectorPort == null) {
			throw new TaskAbortException("Invalid runtime %s. Missing port attribute from server.xml file.", runtimeName);
		}

		try {
			int port = Integer.parseInt(connectorPort);
			context.put("runtime.port", port);
		} catch (NumberFormatException e) {
			throw new TaskAbortException("Invalid runtime %s. Not numeric port attribute on server.xml file.", runtimeName);
		}

		context.put("runtime.name", runtimeName);
		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "Configure runtime server connection.";
	}

	@Override
	public String getDisplay() {
		return "Bind Runtime";
	}
}
