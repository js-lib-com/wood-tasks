package com.jslib.wood.tasks;

import js.log.Log;
import js.log.LogFactory;

public class StopRuntime extends BaseRuntimeScript {
	private static final Log log = LogFactory.getLog(StartRuntime.class);

	public StopRuntime() {
		super("Stoping", "shutdown");
		log.trace("StopRuntime()");
	}

	@Override
	public String getDescription() {
		return "Stop runtime defined for this WOOD project.";
	}

	@Override
	public String getDisplay() {
		return "Stop Runtime";
	}
}
