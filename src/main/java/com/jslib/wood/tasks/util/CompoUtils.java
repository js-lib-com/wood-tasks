package com.jslib.wood.tasks.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;

import javax.inject.Inject;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.docore.IFiles;
import com.jslib.dospi.TaskAbortException;

public class CompoUtils {
	private static final Log log = LogFactory.getLog(CompoUtils.class);

	private final IFiles files;

	@Inject
	public CompoUtils(IFiles files) {
		log.trace("CompoUtils(files)");
		this.files = files;
	}

	public Path findCompoByPageName(String pageName) throws IOException, TaskAbortException {
		class FindResult {
			Path value = null;
		}

		final FindResult findResult = new FindResult();
		final Path projectDir = Paths.get("").toAbsolutePath();
		Files.walkFileTree(projectDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (dir.getFileName().toString().equals(pageName) && files.isXML(dir.resolve(pageName + ".xml"), "page")) {
					findResult.value = projectDir.relativize(dir);
					return FileVisitResult.TERMINATE;
				}
				return FileVisitResult.CONTINUE;
			}
		});

		if (findResult.value == null) {
			throw new TaskAbortException("Page %s not found.", pageName);
		}
		return findResult.value;
	}
}
