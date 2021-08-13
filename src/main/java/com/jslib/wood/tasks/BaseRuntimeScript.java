package com.jslib.wood.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import com.jslib.dospi.IParameters;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;

import js.log.Log;
import js.log.LogFactory;

public abstract class BaseRuntimeScript extends WoodTask {
	private static final Log log = LogFactory.getLog(BaseRuntimeScript.class);

	private final String action;
	private final String scriptName;
	private final ProcessBuilder builder;

	public BaseRuntimeScript(String action, String scriptName) {
		super();
		log.trace("BaseRuntimeScript(action, scriptName)");
		this.action = action;
		this.scriptName = scriptName;
		this.builder = new ProcessBuilder();
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Exception {
		log.trace("execute(parameters)");
		if (System.getProperty("java.home") == null) {
			throw new TaskAbortException("Missing JAVA_HOME environment variable.");
		}

		File runtimeDir = new File(context.getex("runtime.home", File.class), context.getex("runtime.name"));
		if (!runtimeDir.exists()) {
			throw new TaskAbortException("Missing runtime directory %s.", runtimeDir);
		}

		File binDir = new File(runtimeDir, "bin");
		if (!binDir.exists()) {
			throw new TaskAbortException("Invalid runtime %s. Missing binaries director.", runtimeDir);
		}
		File startupScript = new File(binDir, scriptName + getExtension());
		if (!startupScript.exists()) {
			throw new TaskAbortException("Invalid runtime %s. Missing startup script %s.", runtimeDir, startupScript);
		}

		List<String> command = Arrays.asList(startupScript.getAbsolutePath());
		builder.command(command);
		builder.environment().put("CATALINA_HOME", runtimeDir.getAbsolutePath());
		builder.redirectErrorStream(true);

		log.info("%s runtime %s...", action, runtimeDir);
		Process process = builder.start();

		Thread stdinReader = new Thread(() -> {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				// when process exits its standard output is closed and input reader returns null
				while ((line = in.readLine()) != null) {
					log.debug(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		stdinReader.start();

		int exitCode = process.waitFor();
		if (exitCode != 0) {
			log.warn("Error on runtime startup. Exit code: %d.", exitCode);
		}
		stdinReader.join(4000);
		log.debug("Standard input reader thread closed.");

		return ReturnCode.SUCCESS;
	}

	private static String getExtension() {
		String osName = System.getProperty("os.name");
		if (osName == null) {
			osName = "Windows";
		}
		return osName.startsWith("Windows") ? ".bat" : ".sh";
	}
}
