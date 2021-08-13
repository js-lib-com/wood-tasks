package com.jslib.wood.tasks;

import javax.inject.Inject;

import com.jslib.docore.IFiles;
import com.jslib.dospi.IShell;

public class ListTemplates extends BaseListComponents {
	@Inject
	public ListTemplates(IShell shell, IFiles files) {
		super(shell, files);
	}

	@Override
	protected String descriptorRoot() {
		return "template";
	}

	@Override
	public String getDescription() {
		return "List project templates.";
	}

	@Override
	public String getDisplay() {
		return "List Templates";
	}
}
