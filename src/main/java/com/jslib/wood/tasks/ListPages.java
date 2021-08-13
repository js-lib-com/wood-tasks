package com.jslib.wood.tasks;

import javax.inject.Inject;

import com.jslib.docore.IFiles;
import com.jslib.dospi.IShell;

public class ListPages extends BaseListComponents {
	@Inject
	public ListPages(IShell shell, IFiles files) {
		super(shell, files);
	}

	@Override
	protected String descriptorRoot() {
		return "page";
	}

	@Override
	public String getDescription() {
		return "List project pages.";
	}

	@Override
	public String getDisplay() {
		return "List Pages";
	}
}
