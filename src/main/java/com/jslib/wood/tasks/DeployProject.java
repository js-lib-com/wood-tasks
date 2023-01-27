package com.jslib.wood.tasks;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.inject.Inject;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.docore.IFiles;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;
import com.jslib.io.FilesIterator;
import com.jslib.io.FilesOutputStream;
import com.jslib.io.StreamHandler;
import com.jslib.lang.GType;
import com.jslib.net.client.HttpRmiClient;
import com.jslib.util.Files;

public class DeployProject extends WoodTask {
	private static final Log log = LogFactory.getLog(DeployProject.class);

	private static final String APPS_MANAGER_CLASS = "com.jslib.wood.apps.AppsManager";

	/**
	 * Optional, potential harmful flag, to force removing of all stale files from target directory. If
	 * <code>removeStaleFiles</code> flag is true, all descendant files from target directory that are not present into source
	 * directory are permanently removed. Depending on usage pattern, this may be potentially harmful for which reason removing
	 * stale files is optional and default to false.
	 */
	private boolean removeStaleFiles;

	private final IFiles files;

	@Inject
	public DeployProject(IFiles files) {
		super();
		log.trace("DeployProject(files)");
		this.files = files;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Throwable {
		log.trace("execute(parameters)");

		Path projectDir = getProjectDir(files);
		Path buildPath = projectDir.resolve(context.getex("build.dir"));
		if (!files.exists(buildPath)) {
			throw new TaskAbortException("Missing build directory %s.", buildPath);
		}
		if (files.isEmpty(buildPath)) {
			throw new TaskAbortException("Empty build directory %s.", buildPath);
		}

		File buildDir = buildPath.toFile();
		String projectName = files.getFileName(projectDir);

		log.info("Compute build files digest.");
		SortedMap<String, byte[]> sourceFiles = new TreeMap<String, byte[]>();
		for (String file : FilesIterator.getRelativeNamesIterator(buildDir)) {
			sourceFiles.put(Files.path2unix(file), Files.getFileDigest(new File(buildDir, file)));
		}

		String server = format("http://%s/server", context.getex("dev.server"));
		log.info("Get dirty files list from server %s", server);
		HttpRmiClient rmi = new HttpRmiClient(server, APPS_MANAGER_CLASS);
		rmi.setReturnType(new GType(List.class, String.class));
		rmi.setExceptions(IOException.class);

		final List<String> dirtyFiles = rmi.invoke("getDirtyFiles", projectName, sourceFiles, removeStaleFiles);
		if (dirtyFiles.isEmpty()) {
			throw new TaskAbortException("No files to deploy.");
		}
		dirtyFiles.forEach(dirtyFile -> log.info("- %s", dirtyFile));

		log.info("Synchronize dirty files on server %s", server);
		rmi = new HttpRmiClient(server, APPS_MANAGER_CLASS);
		rmi.setExceptions(IOException.class);

		rmi.invoke("synchronize", projectName, new StreamHandler<FilesOutputStream>(FilesOutputStream.class) {
			@Override
			protected void handle(FilesOutputStream files) throws IOException {
				files.addFiles(buildDir, dirtyFiles);
			}
		});
		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "Deploy project on development server.";
	}

	@Override
	public String getDisplay() {
		return "Deploy Project";
	}
}
