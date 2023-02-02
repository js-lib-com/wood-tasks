package com.jslib.wood.tasks;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.ReturnCode;

public class CreateFavicon extends WoodTask {
	private static final Log log = LogFactory.getLog(CreateFavicon.class);

	public CreateFavicon() {
		super();
		log.trace("CreateFavicon()");
	}
	
	@Override
	public String getDisplay() {
		return "Create Favicon";
	}

	@Override
	public String getDescription() {
		return "Create and update application favicon.";
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Throwable {
		return ReturnCode.NO_COMMAND;
	}
}
