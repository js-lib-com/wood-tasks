package com.jslib.wood.tasks;

import java.util.HashMap;
import java.util.Map;

import com.jslib.dospi.TaskReference;

public class Repository {
	public static final Map<String, TaskReference> TASKS = new HashMap<>();
	static {
		TASKS.put("bind runtime", new TaskReference(BindRuntime.class, true));
		TASKS.put("build project", new TaskReference(BuildProject.class, true));
		TASKS.put("clean project", new TaskReference(CleanProject.class, true));
		TASKS.put("create icons", new TaskReference(CreateIcons.class, true));
		TASKS.put("create page", new TaskReference(CreatePage.class, true));
		TASKS.put("delete compo", new TaskReference(DeleteComponent.class, true));
		TASKS.put("deploy project", new TaskReference(DeployProject.class, true));
		TASKS.put("export compo", new TaskReference(ExportComponent.class, true));
		TASKS.put("import compo", new TaskReference(ImportComponent.class, true));
		TASKS.put("list compo usage", new TaskReference(ListComponentUsage.class, true));
		TASKS.put("list compos", new TaskReference(ListComponents.class, true));
		TASKS.put("list pages", new TaskReference(ListPages.class, true));
		TASKS.put("list properties", new TaskReference(ListProperties.class, true));
		TASKS.put("list templates", new TaskReference(ListTemplates.class, true));
		TASKS.put("move compo", new TaskReference(MoveComponent.class, true));
		TASKS.put("open page", new TaskReference(OpenPage.class, true));
		TASKS.put("preview compo", new TaskReference(PreviewComponent.class, true));
		TASKS.put("preview page", new TaskReference(PreviewPage.class, true));
		TASKS.put("rename compo", new TaskReference(RenameComponent.class, true));
		TASKS.put("rename variable", new TaskReference(RenameVariable.class, true));
		TASKS.put("start runtime", new TaskReference(StartRuntime.class, true));
		TASKS.put("stop runtime", new TaskReference(StopRuntime.class, true));
		TASKS.put("update runtime", new TaskReference(UpdateRuntime.class, true));

		TASKS.put("clean build project", new TaskReference("file:clean-build-project.do", false));
		TASKS.put("clean deploy project", new TaskReference("file:clean-deploy-project.do", false));
	}
}
