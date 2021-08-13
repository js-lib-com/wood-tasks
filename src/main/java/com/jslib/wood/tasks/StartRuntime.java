package com.jslib.wood.tasks;

import js.log.Log;
import js.log.LogFactory;

public class StartRuntime extends BaseRuntimeScript {
	private static final Log log = LogFactory.getLog(StartRuntime.class);

	public StartRuntime() {
		super("Starting", "startup");
		log.trace("StartRuntime()");
	}

	@Override
	public String getDescription() {
		return "Start runtime defined for this WOOD project.";
	}

	@Override
	public String getDisplay() {
		return "Start Runtime";
	}
}
