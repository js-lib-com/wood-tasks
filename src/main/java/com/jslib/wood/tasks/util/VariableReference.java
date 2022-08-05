package com.jslib.wood.tasks.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jslib.converter.Converter;
import com.jslib.util.Strings;

public class VariableReference implements Converter {
	private static final Pattern PATTERN = Pattern.compile("^\\@([a-z]+)\\/([a-z\\-]+)$");

	private final String value;
	private final String type;
	private final String name;

	public VariableReference() {
		this.value = null;
		this.type = null;
		this.name = null;
	}

	private VariableReference(String value) {
		this.value = value;
		Matcher matcher = PATTERN.matcher(value);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Value format: '@string/title', where 'string' is variable type and 'title' variable name.");
		}
		this.type = matcher.group(1);
		this.name = matcher.group(2);
	}

	public String value() {
		return value;
	}

	public String type() {
		return type;
	}

	public String name() {
		return name;
	}

	public VariableReference clone(String newname) {
		return new VariableReference(Strings.concat('@', type, '/', newname));
	}

	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	@Override
	public <T> T asObject(String string, Class<T> valueType) {
		return (T) new VariableReference(string);
	}

	@Override
	public String asString(Object object) {
		throw new UnsupportedOperationException();
	}
}
