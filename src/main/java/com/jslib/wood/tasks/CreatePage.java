package com.jslib.wood.tasks;

import static java.lang.String.format;
import static js.util.Strings.concat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import com.jslib.docore.IFiles;
import com.jslib.docore.TemplateProcessor;
import com.jslib.dospi.IForm;
import com.jslib.dospi.IFormData;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.IShell;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.util.Classes;
import js.util.Files;
import js.util.Strings;
import js.wood.WOOD;

public class CreatePage extends WoodTask {
	private static final Log log = LogFactory.getLog(CreatePage.class);

	private final IShell shell;
	private final IFiles files;
	private final TemplateProcessor templateProcessor;

	@Inject
	public CreatePage(IShell shell, IFiles files) {
		super();
		log.trace("CreatePage(shell, files)");
		this.shell = shell;
		this.files = files;
		this.templateProcessor = new TemplateProcessor();
	}

	@Override
	public IParameters parameters() {
		log.trace("parameters()");
		IParameters parameters = super.parameters();
		parameters.define(0, "page-path", String.class);
		parameters.define(1, "template-path", String.class);
		return parameters;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Exception {
		log.trace("execute(parameters)");

		String pagePath = parameters.get("page-path");
		String templatePath = parameters.get("template-path");
		log.info("Create page %s based on %s.", pagePath, templatePath);

		Path projectDir = files.getProjectDir();
		Path compoDir = projectDir.resolve(pagePath);
		if (files.exists(compoDir)) {
			throw new TaskAbortException("Page %s already exists.", compoDir);
		}

		files.createDirectory(compoDir);
		String compoName = files.getFileName(compoDir);
		String className = Strings.toMemberName(compoName);

		Path compoTemplateDir = projectDir.resolve(templatePath);
		if (!files.exists(compoTemplateDir)) {
			throw new TaskAbortException("Missing component template %s.", compoTemplateDir);
		}

		Path templateLayoutFile = files.getFileByExtension(compoTemplateDir, ".htm");
		if (templateLayoutFile == null) {
			throw new TaskAbortException("Missing layout file for component template %s.", compoTemplateDir);
		}

		TemplateDocument templateDoc = createTemplateDocument(context.getex("project.operators"), files.getReader(templateLayoutFile));
		if (!templateDoc.hasEditable()) {
			throw new TaskAbortException("Bad template component %s. Missing editable element.", compoTemplateDir);
		}

		List<String> params = new ArrayList<>();
		Pattern paramPattern = Pattern.compile("\\@param\\/([a-z]+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		Matcher paramMatcher = paramPattern.matcher(Strings.load(files.getReader(templateLayoutFile)));
		if (paramMatcher.find()) {
			IForm form = shell.getForm();
			form.setLegend("Template parameters");

			for (int i = 0; i < paramMatcher.groupCount(); ++i) {
				String param = paramMatcher.group(i + 1);
				form.addField(param, String.class);
			}

			IFormData data = form.submit();
			for (String fieldName : data) {
				params.add(concat(fieldName, ":", format("template %s", data.get(fieldName))));
			}
		}

		Map<String, String> variables = new HashMap<>();
		variables.put("page", compoName);

		variables.put("author", context.get("user.name"));
		variables.put("package", context.get("project.package"));
		variables.put("class", compoName);
		variables.put("className", className);
		variables.put("templateAttr", templateDoc.getTemplateAttrName());
		variables.put("templatePath", templatePath);
		variables.put("templateParams", templateDoc.getParamAttr(Strings.join(params, ';')));
		variables.put("xmlns", templateDoc.getXmlnsAttr());

		StringBuilder scriptPath = new StringBuilder();
		if (context.has("script.dir")) {
			scriptPath.append(context.getex("script.dir"));
			scriptPath.append('/');
		}
		if (context.has("project.package")) {
			scriptPath.append(context.getex("project.package").replace('.', '/'));
			scriptPath.append('/');
		}
		scriptPath.append(className);
		scriptPath.append(".js");

		if ("true".equalsIgnoreCase(context.get("compo.script"))) {
			variables.put("compo-script", "true");
		} else {
			variables.put("scriptPath", scriptPath.toString());
		}

		if (templateDoc.hasEditable()) {
			variables.put("tag", templateDoc.getEditableTag());
			variables.put("editable", templateDoc.getEditableOperand());
		} else {
			variables.put("tag", templateDoc.getRootTag());
		}

		variables.put("root", "page");
		variables.put("groupId", "com.js-lib");
		variables.put("artifactId", Files.basename(templatePath));
		variables.put("version", "1.0");
		variables.put("title", Strings.toTitleCase(Strings.concat(compoName, " page")));
		variables.put("description", Strings.concat(Strings.toTitleCase(compoName), " page", " description."));

		templateProcessor.setTargetDir(compoDir.toFile());
		templateProcessor.exec(shell.getHomeDir().toFile(), "compo", "page", variables);

		Reader reader = templateProcessor.getExcludedFileReader(compoName + ".js");
		if (reader != null) {
			Path scriptFile = projectDir.resolve(scriptPath.toString());
			files.copy(reader, files.getWriter(scriptFile));
		}

		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "Create page based on template.";
	}

	@Override
	public String getDisplay() {
		return "Create Page";
	}

	private static TemplateDocument createTemplateDocument(String namingStrategy, Reader reader) throws IOException, SAXException {
		switch (namingStrategy) {
		case "XMLNS":
			return new XmlnsTemplateDoc(reader);
		case "DATA_ATTR":
			return new DataAttrTemplateDoc(reader);
		case "ATTR":
			return new AttrTemplateDoc(reader);
		default:
			throw new BugError("Invalid naming strategy %s.", namingStrategy);
		}
	}

	private static abstract class TemplateDocument {
		private final DocumentBuilder docBuilder = Classes.loadService(DocumentBuilder.class);
		protected final Document doc;
		protected final Element editable;

		protected TemplateDocument(Reader reader) throws IOException, SAXException {
			this.doc = docBuilder.loadXMLNS(reader);
			this.editable = getEditable();
		}

		public String getRootTag() {
			return doc.getRoot().getTag();
		}

		public boolean hasEditable() {
			return editable != null;
		}

		public String getEditableTag() {
			return editable.getTag();
		}

		protected abstract Element getEditable();

		public abstract String getEditableOperand();

		public abstract String getTemplateAttrName();

		public abstract String getParamAttr(String value);

		public abstract String getXmlnsAttr();
	}

	private static class XmlnsTemplateDoc extends TemplateDocument {
		protected XmlnsTemplateDoc(Reader reader) throws IOException, SAXException {
			super(reader);
		}

		@Override
		protected Element getEditable() {
			try {
				return doc.getByXPathNS(WOOD.NS, "//*[@wood:editable]");
			} catch (XPathExpressionException e) {
				// hard coded XPath expression is invalid
				throw new BugError(e);
			}
		}

		@Override
		public String getEditableOperand() {
			return editable.getAttrNS(WOOD.NS, "editable");
		}

		@Override
		public String getTemplateAttrName() {
			return "w:template";
		}

		@Override
		public String getParamAttr(String value) {
			return format("w:param=\"%s\"", value);
		}

		@Override
		public String getXmlnsAttr() {
			return format("xmlns:w=\"%s\"", WOOD.NS);
		}
	}

	private static class DataAttrTemplateDoc extends TemplateDocument {
		protected DataAttrTemplateDoc(Reader reader) throws IOException, SAXException {
			super(reader);
		}

		@Override
		protected Element getEditable() {
			return doc.getByAttr("data-editable");
		}

		@Override
		public String getEditableOperand() {
			return editable.getAttr("data-editable");
		}

		@Override
		public String getTemplateAttrName() {
			return "data-template";
		}

		@Override
		public String getParamAttr(String value) {
			return format("data-param=\"%s\"", value);
		}

		@Override
		public String getXmlnsAttr() {
			return "";
		}
	}

	private static class AttrTemplateDoc extends TemplateDocument {
		protected AttrTemplateDoc(Reader reader) throws IOException, SAXException {
			super(reader);
		}

		@Override
		protected Element getEditable() {
			return doc.getByAttr("editable");
		}

		@Override
		public String getEditableOperand() {
			return editable.getAttr("editable");
		}

		@Override
		public String getTemplateAttrName() {
			return "template";
		}

		@Override
		public String getParamAttr(String value) {
			return format("param=\"%s\"", value);
		}

		@Override
		public String getXmlnsAttr() {
			return "";
		}
	}
}
