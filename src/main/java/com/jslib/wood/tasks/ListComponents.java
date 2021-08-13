package com.jslib.wood.tasks;

import javax.inject.Inject;

import com.jslib.docore.IFiles;
import com.jslib.dospi.IShell;

public class ListComponents extends BaseListComponents {
	@Inject
	public ListComponents(IShell shell, IFiles files) {
		super(shell, files);
	}

	@Override
	protected String descriptorRoot() {
		return "compo";
	}

	@Override
	public String getDescription() {
		return "List project child components.";
	}

	@Override
	public String getDisplay() {
		return "List Components";
	}
}
