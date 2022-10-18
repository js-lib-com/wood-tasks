package com.jslib.wood.tasks;

import static com.jslib.tools.ToolProcess.buildCommand;
import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Path;

import javax.inject.Inject;

import com.jslib.api.json.Json;
import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.docore.IFiles;
import com.jslib.dospi.IConsole;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.IShell;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;
import com.jslib.tools.IResultParser;
import com.jslib.tools.imagick.Color;
import com.jslib.tools.imagick.ConvertProcess;
import com.jslib.util.Classes;
import com.jslib.util.Strings;
import com.jslib.wood.tasks.util.TaskContext;

public class CreateIcons extends WoodTask {
	private static final Log log = LogFactory.getLog(CreateIcons.class);

	private static int[] ICON_VARIANTS = new int[] { 96 };

	private final IConsole console;
	private final IFiles files;
	private final Json json;
	private final ConvertProcess convert;

	@Inject
	public CreateIcons(IShell shell, IFiles files) {
		super();
		log.trace("CreateIcons(files)");
		this.console = shell.getConsole();
		this.files = files;
		this.json = Classes.loadService(Json.class);
		this.convert = new ConvertProcess();
	}

	CreateIcons(TaskContext context, IShell shell, IFiles files, Json json, ConvertProcess convert) {
		super(context);
		log.trace("CreateIcons(files)");
		this.console = shell.getConsole();
		this.files = files;
		this.json = json;
		this.convert = convert;
	}

	@Override
	public IParameters parameters() {
		log.trace("parameters()");
		IParameters parameters = super.parameters();

		Color backgroundColor = Color.random();
		parameters.define("background-color", Color.class, backgroundColor);
		parameters.define("text-color", Color.class, textColor(backgroundColor));
		parameters.define("sphere-effect", Boolean.class, false);

		return parameters;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Exception {
		log.trace("execute(parameters)");
		Path projectDir = getProjectDir(files);
		String projectName = files.getFileName(projectDir);

		Path assetDir = projectDir.resolve(context.getex("asset.dir"));
		if (!files.exists(assetDir)) {
			throw new TaskAbortException("Bad asset dir configuration: %s.", assetDir);
		}

		Path baseIcon = assetDir.resolve("app-icon-512.png");
		if (files.exists(baseIcon)) {
			console.confirm("Overwrite existing icons");
		}

		boolean defaults = false;
		Color backgroundColor = defaults ? Color.random() : parameters.get("background-color", Color.class);
		Color textColor = defaults ? textColor(backgroundColor) : parameters.get("text-color", Color.class);
		boolean sphereEffect = defaults ? false : parameters.get("sphere-effect", Boolean.class);

		Path backgroundFile = projectDir.resolve("background.png");
		log.info("Create icon background.");
		imagick("-size 512x512 xc:none -fill ${color} -draw \"circle 256,256 256,1\" ${file}", backgroundColor, backgroundFile);

		if (sphereEffect) {
			log.info("Apply sphere effect.");
			Path lightEffectFile = projectDir.resolve("light-effect.png");
			imagick("-size 512x512 canvas:none -draw \"circle 256,256 256,146\" -negate -channel A -gaussian-blur 0x80 ${file}", lightEffectFile);
			imagick("-composite -compose atop -geometry -95-124 ${source} ${effect} ${target}", backgroundFile, lightEffectFile, backgroundFile);
			files.delete(lightEffectFile);
		}

		Path textFile = projectDir.resolve("text.png");
		String label = projectName.substring(0, 2).toUpperCase();
		log.info("Write icon text.");
		if (sphereEffect) {
			imagick("-size 512x512 -background transparent -fill ${color} -pointsize 300 -wave 50x1024 -gravity center caption:${label} ${file}", textColor, label, textFile);
			imagick("-composite -geometry -0-50 ${background} ${text} ${icon}", backgroundFile, textFile, baseIcon);
		} else {
			imagick("-size 512x512 -background transparent -fill ${color} -pointsize 256 -gravity center caption:${label} ${file}", textColor, label, textFile);
			imagick("-composite ${background} ${text} ${icon}", backgroundFile, textFile, baseIcon);
		}

		files.delete(backgroundFile);
		files.delete(textFile);

		int[] variants = variants(projectDir);
		for (int variant : variants) {
			log.info("Create icon variant %d.", variant);
			Path icon = assetDir.resolve(format("app-icon-%d.png", variant));
			String w = Integer.toString(variant);
			String h = Integer.toString(variant);
			imagick("${imageFile} -resize ${width}x${height} ${targetFile}", baseIcon, w, h, icon);
		}

		log.info("Create featured image.");
		backgroundFile = projectDir.resolve("background.png");
		// reverse order of width and height because of -rotate 90
		imagick("-size 500x952 xc:red -colorspace HSB gradient: -compose CopyRed -composite -colorspace RGB -rotate 90.0 ${file}", backgroundFile);
		imagick("${source} -brightness-contrast -30x-40 ${target}", backgroundFile, backgroundFile);

		Path iconFile = assetDir.resolve("app-icon-256.png");
		if (!files.exists(iconFile)) {
			imagick("${imageFile} -resize 256x256 ${targetFile}", baseIcon, iconFile);
		}
		Path featuredFile = assetDir.resolve("app-featured.png");
		// offset +122 is adjusted for icon size of 256: (500 - 256) / 2
		imagick("-composite -compose atop -geometry +122+122 ${background} ${icon} ${featured}", backgroundFile, iconFile, featuredFile);

		// remove files created for internal use if are not in requested variants
		if (!isVariant(variants, 512)) {
			files.delete(assetDir.resolve("app-icon-512.png"));
		}
		if (!isVariant(variants, 256)) {
			files.delete(assetDir.resolve("app-icon-256.png"));
		}
		files.delete(backgroundFile);

		return ReturnCode.SUCCESS;
	}

	private static boolean isVariant(int[] variants, int variant) {
		for (int i = 0; i < variants.length; ++i) {
			if (variants[i] == variant) {
				return true;
			}
		}
		return false;
	}

	private static class Icon {
		String sizes;
	}

	private static class Manifest {
		Icon[] icons;
	}

	private int[] variants(Path projectDir) throws IOException, TaskAbortException {
		String manifestPath = context.get("pwa.manifest");
		log.debug("pwa.manifest: %s", manifestPath);
		if (manifestPath == null) {
			return ICON_VARIANTS;
		}

		Path manifestFile = projectDir.resolve(manifestPath);
		if (!files.exists(manifestFile)) {
			return ICON_VARIANTS;
		}

		Manifest manifest = json.parse(files.getReader(manifestFile), Manifest.class);

		int[] variants = new int[manifest.icons.length];
		for (int i = 0; i < variants.length; ++i) {
			String sizes = manifest.icons[i].sizes;
			int xposition = sizes.indexOf('x');
			variants[i] = Integer.parseInt(sizes.substring(0, xposition));
			log.debug("Icon size variant: %d", variants[i]);
		}
		return variants;
	}

	@Override
	public String getDescription() {
		return "Create or update application icons.";
	}

	@Override
	public String getDisplay() {
		return "Create Icons";
	}

	@Override
	public String help() throws IOException {
		log.trace("hep()");
		return Strings.load(Classes.getResourceAsReader("manual/create-icons.md"));
	}

	// --------------------------------------------------------------------------------------------

	void imagick(String command, Object... args) throws IOException, TaskAbortException {
		imagick(null, command, args);
	}

	private <T extends IResultParser> T imagick(Class<T> resultType, String parameterizedCommand, Object... args) throws IOException, TaskAbortException {
		// cannot set binary path on constructor because config is not initialized there
		ConvertProcess.setPath(context.getex("imagick.convert.path"));

		String command = buildCommand(parameterizedCommand, args);
		log.debug(command);
		convert.setTimeout(30000L);
		if (resultType == null) {
			convert.exec(command);
			return null;
		}
		return convert.exec(command, resultType);
	}

	private Color textColor(Color color) {
		float[] hsb = color.hsb();
		return hsb[2] >= 0.5 ? Color.Black : Color.White;
	}
}
