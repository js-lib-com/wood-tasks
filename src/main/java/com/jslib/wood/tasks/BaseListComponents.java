package com.jslib.wood.tasks;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.docore.IFiles;
import com.jslib.dospi.Flags;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.IPrintout;
import com.jslib.dospi.IShell;
import com.jslib.dospi.ReturnCode;
import com.jslib.format.FileSize;

public abstract class BaseListComponents extends WoodTask {
	private static final Log log = LogFactory.getLog(BaseListComponents.class);

	private final IShell shell;
	private final IFiles files;

	public BaseListComponents(IShell shell, IFiles files) {
		super();
		log.trace("BaseListComponents(shell, files)");
		this.shell = shell;
		this.files = files;
	}

	@Override
	public IParameters parameters() {
		log.trace("parameters()");
		IParameters parameters = super.parameters();
		parameters.define(0, "display-mode", Flags.ARGUMENT, DisplayMode.class, DisplayMode.brief);
		return parameters;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Exception {
		log.trace("execute(parameters)");

		Path workingDir = files.getProjectDir();
		IPrintout printout = shell.getPrintout();
		DisplayMode displayMode = parameters.get("display-mode", DisplayMode.class);

		printout.createUnorderedList();
		files.walkFileTree(workingDir, new FileVisitor(workingDir, printout, displayMode));
		printout.display();

		return ReturnCode.SUCCESS;
	}

	protected abstract String descriptorRoot();

	protected class FileVisitor extends SimpleFileVisitor<Path> {
		private final DateTimeFormatter MODIFIED_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		private final FileSize FILE_SIZE_FORMAT = new FileSize();

		private final Path workingDir;
		private final IPrintout printout;
		private final DisplayMode displayMode;

		private FileTime modifiedTime;
		private long dirSize;

		public FileVisitor(Path workingDir, IPrintout printout, DisplayMode displayMode) {
			this.workingDir = workingDir;
			this.printout = printout;
			this.displayMode = displayMode;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			modifiedTime = attrs.lastModifiedTime();
			dirSize = 0;
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			dirSize += attrs.size();
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			if (files.isXML(dir.resolve(dir.getFileName() + ".xml"), descriptorRoot())) {
				String compoPath = workingDir.relativize(dir).toString().replace('\\', '/');
				if (displayMode == DisplayMode.details) {
					LocalDateTime dt = modifiedTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
					printout.addListItem(String.format("%s %10s %s", dt.format(MODIFIED_TIME_FORMAT), FILE_SIZE_FORMAT.format(dirSize), compoPath));
				} else {
					printout.addListItem(compoPath);
				}
			}
			return FileVisitResult.CONTINUE;
		}
	}

	protected enum DisplayMode {
		brief, details
	}
}
