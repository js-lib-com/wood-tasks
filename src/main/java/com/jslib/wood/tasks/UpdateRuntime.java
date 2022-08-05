package com.jslib.wood.tasks;

import java.net.URI;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import javax.inject.Inject;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.docore.IApacheIndex;
import com.jslib.docore.IFiles;
import com.jslib.docore.IHttpFile;
import com.jslib.docore.IHttpRequest;
import com.jslib.docore.IProgress;
import com.jslib.dospi.IConsole;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.IShell;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;
import com.jslib.format.FileSize;

public class UpdateRuntime extends WoodTask {
	private static final Log log = LogFactory.getLog(UpdateRuntime.class);

	private static final URI DISTRIBUTION_URI = URI.create("http://maven.js-lib.com/com/js-lib/");
	private static final Pattern DIRECTORY_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d*(?:-[a-z0-9]+)?/$", Pattern.CASE_INSENSITIVE);
	// wood-core-1.1.0-20210422.044635-1.jar
	private static final Pattern FILE_PATTERN = Pattern.compile("^wood-(?:core|preview)-(?:[0-9.-]+).jar$");

	private static final DateTimeFormatter modificationTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final FileSize fileSizeFormatter = new FileSize();

	private final IShell shell;
	private final IFiles files;
	private final IHttpRequest http;
	private final IApacheIndex apache;

	@Inject
	public UpdateRuntime(IShell shell, IFiles files, IHttpRequest http, IApacheIndex apache) {
		log.trace("UpdateRuntime(shell, files, http, apache)");
		this.shell = shell;
		this.files = files;
		this.http = http;
		this.apache = apache;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Exception {
		log.trace("execute(parameters)");

		Path runtimeDir = files.getPath(context.getex("runtime.home")).resolve(context.getex("runtime.name"));
		if (!files.exists(runtimeDir)) {
			throw new TaskAbortException("Missing runtime directory %s.", runtimeDir);
		}
		Path libDir = runtimeDir.resolve("lib");
		if (!files.exists(libDir)) {
			throw new TaskAbortException("Missing runtime library directory %s", libDir);
		}
		log.info("Checking WOOD libraries repository...");

		IProgress<IHttpFile> fileProgress = file -> {
			log.info("%s %s\t%s", modificationTimeFormatter.format(file.getModificationTime()), fileSizeFormatter.format(file.getSize()), file.getName());
		};

		// is critical to end the path with separator
		URI coreURI = DISTRIBUTION_URI.resolve("wood-core/");
		IHttpFile coreDir = apache.scanLatestFileVersion(coreURI, DIRECTORY_PATTERN, fileProgress);
		if (coreDir == null) {
			throw new TaskAbortException("Empty WOOD core library repository %s.", coreURI);
		}
		IHttpFile coreFile = apache.scanLatestFileVersion(coreDir.getURI(), FILE_PATTERN, fileProgress);
		if (coreFile == null) {
			throw new TaskAbortException("Invalid WOOD core version %s. No archive found.", coreDir.getURI());
		}

		// is critical to end the path with separator
		URI previewURI = DISTRIBUTION_URI.resolve("wood-preview/");
		IHttpFile previewDir = apache.scanLatestFileVersion(previewURI, DIRECTORY_PATTERN, fileProgress);
		if (previewDir == null) {
			throw new TaskAbortException("Empty WOOD preview library repository %s.", previewURI);
		}
		IHttpFile previewFile = apache.scanLatestFileVersion(previewDir.getURI(), FILE_PATTERN, fileProgress);
		if (previewFile == null) {
			throw new TaskAbortException("Invalid WOOD preview version %s. No archive found.", previewDir.getURI());
		}

		IConsole console = shell.getConsole();
		console.confirm("Update WOOD libraries: %s, %s", coreFile.getName(), previewFile.getName());

		log.info("Download WOOD core archive %s", coreFile.getName());
		Path downloadedCoreFile = http.download(coreFile.getURI(), libDir.resolve("~" + coreFile.getName()), shell.getProgress(coreFile.getSize()));

		log.info("Download WOOD preview archive %s", previewFile.getName());
		Path downloadedPreviewFile = http.download(previewFile.getURI(), libDir.resolve("~" + previewFile.getName()), shell.getProgress(previewFile.getSize()));

		log.info("Replace WOOD core archive %s", coreFile.getName());
		files.deleteIfExists(files.getFileByNamePattern(libDir, Pattern.compile("^wood-core-\\d+\\.\\d+.+\\.jar$")));
		files.move(downloadedCoreFile, libDir.resolve(coreFile.getName()));

		log.info("Replace WOOD preview archive %s", previewFile.getName());
		files.deleteIfExists(files.getFileByNamePattern(libDir, Pattern.compile("^wood-preview-\\d+\\.\\d+.+\\.jar$")));
		files.move(downloadedPreviewFile, libDir.resolve(previewFile.getName()));

		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "Update WOOD libraries on bound runtime.";
	}

	@Override
	public String getDisplay() {
		return "Update Runtime";
	}
}
