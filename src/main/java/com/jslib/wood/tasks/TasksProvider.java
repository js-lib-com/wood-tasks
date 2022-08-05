package com.jslib.wood.tasks;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.dospi.ITask;
import com.jslib.dospi.ITasksProvider;
import com.jslib.dospi.TaskReference;
import com.jslib.util.Classes;
import com.jslib.wood.tasks.util.CompoUtils;

public class TasksProvider implements ITasksProvider {
	private static final Log log = LogFactory.getLog(TasksProvider.class);

	private static final String NAME = "WOOD";

	public TasksProvider() {
		log.trace("TasksProvider()");
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public List<Class<? extends ITask>> getTasksList() {
		List<Class<? extends ITask>> list = new ArrayList<>();

		list.add(BindRuntime.class);
		list.add(BuildProject.class);
		list.add(CleanProject.class);
		list.add(CreateIcons.class);
		list.add(CreatePage.class);
		list.add(DeleteComponent.class);
		list.add(DeployProject.class);
		list.add(ExportComponent.class);
		list.add(ImportComponent.class);
		list.add(ListComponentUsage.class);
		list.add(ListComponents.class);
		list.add(ListPages.class);
		list.add(ListProperties.class);
		list.add(ListTemplates.class);
		list.add(MoveComponent.class);
		list.add(OpenPage.class);
		list.add(PreviewComponent.class);
		list.add(PreviewPage.class);
		list.add(RenameComponent.class);
		list.add(RenameVariable.class);
		list.add(StartRuntime.class);
		list.add(StopRuntime.class);
		list.add(UpdateRuntime.class);

		return list;
	}

	@Override
	public List<Class<?>> getDependencies() {
		List<Class<?>> dependencies = new ArrayList<>();
		dependencies.add(CompoUtils.class);
		return dependencies;
	}

	@Override
	public Map<String, TaskReference> getTaskReferences() {
		log.trace("getTasks()");
		return Repository.TASKS;
	}

	@Override
	public Reader getScriptReader(TaskReference reference) throws IOException {
		return Classes.getResourceAsReader("script/" + reference.getPath());
	}
}
