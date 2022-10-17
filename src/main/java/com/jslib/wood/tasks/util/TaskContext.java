package com.jslib.wood.tasks.util;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import com.jslib.api.dom.Document;
import com.jslib.api.dom.DocumentBuilder;
import com.jslib.api.dom.Element;
import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.converter.Converter;
import com.jslib.converter.ConverterRegistry;
import com.jslib.dospi.TaskAbortException;
import com.jslib.util.Classes;
import com.jslib.util.Strings;

public class TaskContext {
	private static final Log log = LogFactory.getLog(TaskContext.class);

	private static final String PROPERTIES_FILE = ".wood.properties";
	private static final String DESCRIPTOR_FILE = "project.xml";

	private final Converter converter = ConverterRegistry.getConverter();
	private static final DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);

	private final Path projectPropertiesFile;
	private final Properties properties = new Properties();

	public TaskContext() {
		Path workingDir = Paths.get("").toAbsolutePath();

		Path projectDescriptorFile = workingDir.resolve(DESCRIPTOR_FILE);
		if (Files.exists(projectDescriptorFile)) {
			try (Reader reader = Files.newBufferedReader(projectDescriptorFile)) {
				Document doc = builder.loadXML(reader);
				for (Element element : doc.findByXPath("//*[normalize-space(text())]")) {
					properties.put(key(element), element.getText());
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Path userDir = Paths.get(System.getProperty("user.home"));
		if (Files.exists(userDir)) {
			Path userPropertiesFile = userDir.resolve(PROPERTIES_FILE);
			if (Files.exists(userPropertiesFile)) {
				try (Reader reader = Files.newBufferedReader(userPropertiesFile)) {
					properties.load(reader);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		this.projectPropertiesFile = workingDir.resolve(PROPERTIES_FILE);
		if (Files.exists(projectPropertiesFile)) {
			try (Reader reader = Files.newBufferedReader(projectPropertiesFile)) {
				properties.load(reader);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static String key(Element element) {
		List<String> parts = Strings.split(element.getTag(), '-');
		if (parts.size() == 1) {
			parts.add(0, "project");
		}
		return Strings.join(parts, '.');
	}

	public <T> T get(String key, Class<T> type, String... defaultValue) {
		Object value = properties.get(key);
		if (value == null) {
			if (defaultValue.length == 1) {
				value = defaultValue[0];
			} else {
				return null;
			}
		}
		return converter.asObject(value.toString(), type);
	}

	public <T> T getex(String key, Class<T> type, String... defaultValue) throws TaskAbortException {
		T value = get(key, type, defaultValue);
		if (value == null) {
			throw new TaskAbortException("Missing %s property.", key);
		}
		return value;
	}

	public String get(String key, String... defaultValue) {
		return get(key, String.class, defaultValue);
	}

	public String getex(String key, String... defaultValue) throws TaskAbortException {
		return getex(key, String.class, defaultValue);
	}

	public boolean has(String key) {
		return properties.containsKey(key);
	}

	public Properties properties() {
		return properties;
	}

	public void put(String key, Object value) throws IOException {
		String stringValue = converter.asString(value);
		properties.put(key, stringValue);

		Properties projectProperties = new Properties();
		if (Files.exists(projectPropertiesFile)) {
			try (Reader reader = Files.newBufferedReader(projectPropertiesFile)) {
				projectProperties.load(reader);
			}
		}

		projectProperties.put(key, stringValue);
		try (Writer writer = Files.newBufferedWriter(projectPropertiesFile)) {
			projectProperties.store(writer, "Update " + key);
		}
		log.info("Set property %s to %s.", key, stringValue);
	}
}
