package com.jslib.wood.tasks;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Path;

import javax.inject.Inject;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.xml.sax.SAXException;

import com.jslib.api.dom.Document;
import com.jslib.api.dom.DocumentBuilder;
import com.jslib.api.dom.Element;
import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.docore.IFiles;
import com.jslib.docore.repo.RepositoryCoordinates;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.ReturnCode;
import com.jslib.dospi.TaskAbortException;
import com.jslib.util.Classes;

public class ExportComponent extends WoodTask {
	private static final Log log = LogFactory.getLog(ExportComponent.class);

	private final IFiles files;
	private final HttpClientBuilder clientBuilder;

	@Inject
	public ExportComponent(IFiles files, HttpClientBuilder clientBuilder) {
		super();
		log.trace("ExportComponent(files, clientBuilder)");
		this.files = files;
		this.clientBuilder = clientBuilder;
	}

	@Override
	public IParameters parameters() {
		log.trace("parameters()");
		IParameters parameters = super.parameters();
		parameters.define(0, "component-path", String.class);
		return parameters;
	}

	@Override
	public ReturnCode execute(IParameters parameters) throws Exception {
		log.trace("execute(parameters)");

		String compoPath = parameters.get("component-path");
		log.info("Export component %s.", compoPath);

		Path workingDir = files.getProjectDir();
		Path compoDir = workingDir.resolve(compoPath);
		if (!files.exists(compoDir)) {
			throw new TaskAbortException("Missing component directory %s.", compoDir);
		}

		// WOOD project naming convention: descriptor file basename is the same as component directory
		Path descriptorFile = compoDir.resolve(files.getFileName(compoDir) + ".xml");
		if (!files.exists(descriptorFile)) {
			throw new TaskAbortException("Missing component descriptor %s.", descriptorFile);
		}

		RepositoryCoordinates compoCoordinates = compoCoordinates(descriptorFile);
		if (!compoCoordinates.isValid()) {
			throw new TaskAbortException("Invalid component descriptor %s. Missing component coordinates.", descriptorFile);
		}

		log.info("Cleanup repository component %s.", compoCoordinates);
		cleanupRepositoryComponent(compoCoordinates);

		for (Path compoFile : files.listFiles(compoDir)) {
			log.info("Upload file %s.", compoFile);
			uploadComponentFile(compoFile, compoCoordinates);
		}

		return ReturnCode.SUCCESS;
	}

	@Override
	public String getDescription() {
		return "Export component to repository.";
	}

	@Override
	public String getDisplay() {
		return "Export Component";
	}

	// --------------------------------------------------------------------------------------------

	private void cleanupRepositoryComponent(RepositoryCoordinates coordinates) throws IOException, TaskAbortException {
		String url = String.format("%s/%s/", context.getex("repository.url"), coordinates.toFilePath());
		try (CloseableHttpClient client = clientBuilder.build()) {
			HttpDelete httpDelete = new HttpDelete(url);
			try (CloseableHttpResponse response = client.execute(httpDelete)) {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 404) {
					log.warn("Component %s not existing on repository.", coordinates.toFilePath());
					return;
				}
				if (statusCode != 200) {
					throw new IOException(format("Fail to cleanup component %s", coordinates));
				}
			}
		}
	}

	private void uploadComponentFile(Path compoFile, RepositoryCoordinates coordinates) throws IOException, TaskAbortException {
		String url = String.format("%s/%s/%s", context.getex("repository.url"), coordinates.toFilePath(), files.getFileName(compoFile));
		try (CloseableHttpClient client = clientBuilder.build()) {
			HttpPost httpPost = new HttpPost(url);
			httpPost.setHeader("Content-Type", "application/octet-stream");
			httpPost.setEntity(new InputStreamEntity(files.getInputStream(compoFile)));

			try (CloseableHttpResponse response = client.execute(httpPost)) {
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new IOException(format("Fail to upload file %s", compoFile));
				}
			}
		}
	}

	private RepositoryCoordinates compoCoordinates(Path descriptorFile) throws IOException, SAXException {
		DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);
		Document descriptorDoc = documentBuilder.loadXML(files.getReader(descriptorFile));
		String groupId = text(descriptorDoc, "groupId");
		String artifactId = text(descriptorDoc, "artifactId");
		String version = text(descriptorDoc, "version");
		return new RepositoryCoordinates(groupId, artifactId, version);
	}

	private static String text(Document doc, String tagName) {
		Element element = doc.getByTag(tagName);
		return element != null ? element.getText() : null;
	}
}
