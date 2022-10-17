package com.jslib.wood.tasks;

import static com.jslib.util.Strings.concat;
import static com.jslib.util.Strings.format;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import com.jslib.api.dom.Document;
import com.jslib.api.dom.DocumentBuilder;
import com.jslib.api.dom.Element;
import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.docore.IFiles;
import com.jslib.docore.IHttpRequest;
import com.jslib.docore.repo.RepositoryCoordinates;
import com.jslib.dospi.IConsole;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.IShell;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;
import com.jslib.util.Classes;
import com.jslib.util.Files;
import com.jslib.util.Strings;
import com.jslib.wood.WOOD;
import com.jslib.wood.WoodException;

public class ImportComponent extends WoodTask {
	private static final Log log = LogFactory.getLog(ImportComponent.class);

	/** Pattern for files listed on index page. */
	private static final Pattern FILE_PATTERN = Pattern.compile("^[a-z0-9_.\\-]+\\.[a-z0-9]+$", Pattern.CASE_INSENSITIVE);

	private final IShell shell;
	private final IFiles files;
	private final IHttpRequest httpRequest;
	private final DocumentBuilder documentBuilder;

	private CompoRepository repository;

	@Inject
	public ImportComponent(IShell shell, IFiles files, IHttpRequest httpRequest, DocumentBuilder documentBuilder) {
		super();
		log.trace("ImportComponent(shell, files, httpRequest, documentBuilder)");
		this.shell = shell;
		this.files = files;
		this.httpRequest = httpRequest;
		this.documentBuilder = Classes.loadService(DocumentBuilder.class);
	}

	@Override
	public IParameters parameters() {
		log.trace("parameters()");
		IParameters parameters = super.parameters();
		parameters.define(0, "component-coordinates", RepositoryCoordinates.class);
		return parameters;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Exception {
		log.trace("execute(parameters)");
		repository = new CompoRepository();

		RepositoryCoordinates coordinates = parameters.get("component-coordinates", RepositoryCoordinates.class);
		return importComponent(coordinates);
	}

	@Override
	public String getDescription() {
		return "Import component from repository.";
	}

	@Override
	public String getDisplay() {
		return "Import Component";
	}

	// --------------------------------------------------------------------------------------------

	private ReturnCode importComponent(RepositoryCoordinates compoCoordinates) throws Exception {
		IConsole console = shell.getConsole();

		log.info("Import %s.", compoCoordinates);

		Path projectDir = getProjectDir(files);
		Path projectCompoDir = projectCompoDir(compoCoordinates);
		if (projectCompoDir != null) {
			log.info("Component %s already loaded on %s.", compoCoordinates, compoPath(projectCompoDir));
			return ReturnCode.SUCCESS;
		}

		String path = console.prompt("local path");
		projectCompoDir = projectDir.resolve(path);

		// recursive import of the component's dependencies
		for (RepositoryCoordinates dependency : repository.getCompoDependencies(compoCoordinates)) {
			ReturnCode code = importComponent(dependency);
			if (code != ReturnCode.SUCCESS) {
				return code;
			}
		}

		copyComponent(compoCoordinates, projectCompoDir);

		Path descriptorFile = projectCompoDir.resolve(files.getFileName(projectCompoDir) + ".xml");
		CompoDescriptor compoDescriptor = new CompoDescriptor(files, descriptorFile);

		compoDescriptor.createScripts();
		for (RepositoryCoordinates coordinates : compoDescriptor.getDependencies()) {
			Path projectCompoPath = projectCompoDir(coordinates);
			Path scriptFile = projectCompoPath != null ? compoFile(projectCompoPath, "js") : null;
			compoDescriptor.addScriptDependency(scriptFile);
		}

		compoDescriptor.removeDependencies();
		compoDescriptor.save();

		return ReturnCode.SUCCESS;
	}

	/**
	 * Copy repository component identified by its coordinates to project component directory. While importing into project, it
	 * is acceptable that source repository component to be renamed. If this is the case, this method takes care to rename
	 * layout, style, script and descriptor files.
	 * <p>
	 * After all files are copied into project component, this method takes care to update WOOD operators into layout file(s) -
	 * see {@link #updateLayoutOperators(Path)}.
	 * <p>
	 * Warning: this method remove all target component directory files.
	 * 
	 * @param compoCoordinates coordinates for repository component,
	 * @param projectCompoDir project component directory.
	 * @throws IOException if copy operation fails.
	 */
	void copyComponent(RepositoryCoordinates compoCoordinates, Path projectCompoDir) throws IOException {
		if (!files.exists(projectCompoDir)) {
			files.createDirectory(projectCompoDir);
		}
		files.cleanDirectory(projectCompoDir);

		files.walkFileTree(repository.getCompoDir(compoCoordinates), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String fileName = files.getFileName(file);
				// ensure that original repository component files are renamed using project component directory name
				// this applies to layout, style, script and descriptor files
				if (Files.basename(fileName).equals(compoCoordinates.getArtifactId())) {
					fileName = concat(files.getFileName(projectCompoDir), '.', Files.getExtension(fileName));
				}
				log.info("Copy file %s", file);
				files.copy(file, projectCompoDir.resolve(fileName));
				return FileVisitResult.CONTINUE;
			}
		});

		files.walkFileTree(projectCompoDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (files.getFileName(file).endsWith(".htm")) {
					updateLayoutOperators(file, context.get("project.operators"));
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Post process layout file operators after component files copied to project. This method scan layout document and rename
	 * WOOD operators accordingly project naming strategy, as defined by <code>project.operators</code> configuration property.
	 * Keep in mind that on repository component operators always use <code>XMLNS</code> naming convention.
	 * <p>
	 * Also, if operator value is a reference to a dependency component, find it on project file system and resolve operator
	 * path value. On repository, component references are always represented as component coordinates.
	 * 
	 * @param layoutFile layout file path.
	 * @throws IOException if layout file reading or document parsing fail.
	 */
	void updateLayoutOperators(Path layoutFile, String operatorsNaming) throws IOException {
		boolean hasNamespace = "XMLNS".equals(operatorsNaming);
		String prefix = "DATA_ATTR".equals(operatorsNaming) ? "data-" : "";
		Path projectDir = getProjectDir(files);

		try {
			Document document = documentBuilder.loadXMLNS(files.getReader(layoutFile));

			for (String operator : new String[] { "template", "editable", "content", "compo", "param" }) {
				for (Element element : document.findByAttrNS(WOOD.NS, operator)) {
					String value = element.getAttrNS(WOOD.NS, operator);

					// if value is a component coordinates for a dependency, resolve it to project component path
					RepositoryCoordinates coordinates = RepositoryCoordinates.parse(value);
					if (coordinates != null) {
						// convert project component directory into component path
						// WOOD component path always uses slash ('/') as separator and is always relative to project root
						value = projectDir.relativize(projectCompoDir(coordinates)).toString().replace('\\', '/');
					}

					if (!hasNamespace) {
						element.removeAttrNS(WOOD.NS, operator);
						element.setAttr(prefix + operator, value);
					} else {
						element.setAttrNS(WOOD.NS, operator, value);
					}
				}
			}

			if (!hasNamespace) {
				document.removeNamespaceDeclaration(WOOD.NS);
			}

			// close writer and do not serialize XML declaration
			document.serialize(files.getWriter(layoutFile), true, false);
		} catch (SAXException e) {
			throw new IOException(format("Fail to update operators on layout file |%s|: %s: %s", layoutFile, e.getClass(), e.getMessage()));
		}
	}

	String compoPath(Path compoDir) {
		Path projectDir = getProjectDir(files);
		return projectDir.relativize(compoDir).toString().replace('\\', '/');
	}

	Path compoFile(Path compoDir, String extension) {
		String compoName = files.getFileName(compoDir);
		return compoDir.resolve(concat(compoName, '.', extension));
	}

	Path projectCompoDir(RepositoryCoordinates coordinates) throws IOException {
		class Component {
			Path path = null;
		}
		final Component component = new Component();

		files.walkFileTree(getProjectDir(files), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Path descriptorFile = dir.resolve(files.getFileName(dir) + ".xml");
				if (!files.exists(descriptorFile)) {
					return FileVisitResult.CONTINUE;
				}
				CompoDescriptor descriptor = new CompoDescriptor(files, descriptorFile);
				if (!coordinates.equals(descriptor.getCoordinates())) {
					return FileVisitResult.CONTINUE;
				}
				component.path = dir;
				return FileVisitResult.TERMINATE;
			}
		});

		return component.path;
	}

	static URI URI(String server, String... paths) {
		StringBuilder uri = new StringBuilder(server);
		if (!uri.toString().endsWith("/")) {
			uri.append('/');
		}
		for (String path : paths) {
			uri.append(path.replace('.', '/'));
			if (!path.endsWith("/")) {
				uri.append('/');
			}
		}
		return URI.create(uri.toString());
	}

	// --------------------------------------------------------------------------------------------

	class CompoRepository {
		private final Path repositoryDir;

		private boolean reload;

		public CompoRepository() throws IOException, TaskAbortException {
			this.repositoryDir = files.getPath(context.getex("repository.dir"));
		}

		void setReload(boolean reload) {
			this.reload = reload;
		}

		/**
		 * Get the directory path from local components repository (cache) that contains component identified by given
		 * coordinates.
		 * 
		 * @param coordinates component coordinates.
		 * @return component repository directory.
		 */
		public Path getCompoDir(RepositoryCoordinates coordinates) {
			return repositoryDir.resolve(coordinates.toFilePath());
		}

		/**
		 * Get dependencies declared on component descriptor for component identified by its coordinates. If requested component
		 * does not exist on local repository takes care to download it. If {@link #reload} flag is set on command options,
		 * force download and local cache overwrite.
		 * 
		 * @param coordinates component coordinates.
		 * @return dependencies list, possible empty if there are no dependencies declared on component descriptor.
		 * @throws IOException if a file system operation fails.
		 * @throws XPathExpressionException if there are internal XPath syntax error.
		 * @throws SAXException if downloaded document is not well formed.
		 * @throws TaskAbortException
		 */
		public List<RepositoryCoordinates> getCompoDependencies(RepositoryCoordinates coordinates) throws IOException, XPathExpressionException, SAXException, TaskAbortException {
			Path repositoryCompoDir = repositoryDir.resolve(coordinates.toFilePath());
			if (!files.exists(repositoryCompoDir)) {
				reload = true;
			}
			if (reload) {
				files.createDirectories(repositoryCompoDir);
				files.cleanDirectory(repositoryCompoDir);
				downloadCompoment(coordinates, repositoryCompoDir);
			}

			Path compoDir = repositoryDir.resolve(coordinates.toFilePath());
			Path descriptorFile = compoDir.resolve(coordinates.getArtifactId() + ".xml");
			CompoDescriptor descriptor = new CompoDescriptor(files, descriptorFile);
			return descriptor.getDependencies();
		}

		/**
		 * Download component files from repository into target directory. This method assume repository server is configured
		 * with page indexing. If first loads remote directory index, then scan and download all links matching
		 * {@link #FILE_PATTERN}.
		 * 
		 * @param targetDir target directory.
		 * @throws IOException if download fails for whatever reason.
		 * @throws SAXException
		 * @throws XPathExpressionException
		 * @throws TaskAbortException
		 */
		private void downloadCompoment(RepositoryCoordinates coordinates, Path targetDir) throws IOException, SAXException, XPathExpressionException, TaskAbortException {
			URI indexPage = URI.create((format("%s/%s/", context.getex("repository.url"), coordinates.toFilePath())));
			Document indexPageDoc = httpRequest.loadHTML(indexPage);

			for (Element linkElement : indexPageDoc.findByXPath("//*[@href]")) {
				String link = linkElement.getAttr("href");
				Matcher matcher = FILE_PATTERN.matcher(link);
				if (matcher.find()) {
					URI linkURI = indexPage.resolve(link);
					Path file = targetDir.resolve(Strings.last(link, '/'));
					log.info("Download file %s.", linkURI);
					httpRequest.download(linkURI, file);
					log.info("Copy file %s.", file);
				}
			}
		}
	}

	static class CompoDescriptor {
		private static final DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);

		private final IFiles files;
		private final Path descriptorFile;
		private final Document document;
		private final boolean hasDpendencies;

		private Element scriptElement;

		public CompoDescriptor(IFiles files, Path descriptorFile) throws IOException {
			if (!files.exists(descriptorFile)) {
				throw new WoodException("Missing component descriptor %s.", descriptorFile);
			}
			this.files = files;
			this.descriptorFile = descriptorFile;
			try {
				this.document = documentBuilder.loadXML(files.getReader(descriptorFile));
			} catch (SAXException e) {
				throw new IOException(format("Fail to parse component descriptor |%s|.", descriptorFile));
			}
			this.hasDpendencies = document.getByTag("dependencies") != null;
		}

		public List<RepositoryCoordinates> getDependencies() {
			List<RepositoryCoordinates> dependencies = new ArrayList<>();
			for (Element dependency : document.findByTag("dependency")) {
				dependencies.add(new RepositoryCoordinates(getText(dependency, "groupId"), getText(dependency, "artifactId"), getText(dependency, "version")));
			}
			return dependencies;
		}

		public void createScripts() {
			if (!hasDpendencies) {
				return;
			}
			Element scriptsElement = document.getByTag("scripts");
			if (scriptsElement == null) {
				scriptsElement = document.createElement("scripts");
				document.getRoot().addChild(scriptsElement);
			}
			Path scriptFile = files.changeExtension(descriptorFile, "js");
			scriptElement = document.createElement("script", "src", src(scriptFile));
			scriptsElement.addChild(scriptElement);
		}

		public void addScriptDependency(Path scriptFile) {
			if (scriptElement != null) {
				scriptElement.addChild(document.createElement("dependency", "src", src(scriptFile)));
			}
		}

		private String src(Path scriptFile) {
			return files.getWorkingDir().relativize(scriptFile).toString().replace('\\', '/');
		}

		public RepositoryCoordinates getCoordinates() {
			String groupId = getText("groupId");
			String artifactId = getText("artifactId");
			String version = getText("version");
			return new RepositoryCoordinates(groupId, artifactId, version);
		}

		private String getText(String tagName) {
			Element element = document.getByTag(tagName);
			return element != null ? element.getText() : null;
		}

		private static String getText(Element element, String tagName) {
			Element childElement = element.getByTag(tagName);
			return childElement != null ? childElement.getText() : null;
		}

		public void removeDependencies() {
			if (!hasDpendencies) {
				return;
			}
			Element dependencies = document.getByTag("dependencies");
			if (dependencies != null) {
				dependencies.remove();
			}
		}

		public void save() throws IOException {
			document.serialize(files.getWriter(descriptorFile), true);
		}

		Document getDocument() {
			return document;
		}

		void setScriptElement(Element scriptElement) {
			this.scriptElement = scriptElement;
		}
	}
}
