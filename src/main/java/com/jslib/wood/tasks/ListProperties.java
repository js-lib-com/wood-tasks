package com.jslib.wood.tasks;

import javax.inject.Inject;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.IPrintout;
import com.jslib.dospi.IShell;
import com.jslib.dospi.ReturnCode;

public class ListProperties extends WoodTask {
	private static final Log log = LogFactory.getLog(ListProperties.class);

	private final IShell shell;

	@Inject
	protected ListProperties(IShell shell) {
		super();
		log.trace("ListProperties(shell)");
		this.shell = shell;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Exception {
		log.trace("execute(parameters)");

		IPrintout printout = shell.getPrintout();
		printout.createDefinitionsList();
		context.properties().forEach((key, value) -> {
			printout.addDefinition((String) key, (String) value);
		});
		printout.display();

		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "List WOOD project contextual properties.";
	}

	@Override
	public String getDisplay() {
		return "List Properties";
	}
}
