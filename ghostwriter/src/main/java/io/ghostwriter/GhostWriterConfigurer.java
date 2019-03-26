package io.ghostwriter;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import hu.advancedweb.scott.instrumentation.transformation.ScottClassTransformer;
import hu.advancedweb.scott.instrumentation.transformation.config.Configuration;

public class GhostWriterConfigurer {

	public static Configuration getConfiguration() {
		return new Configuration.Builder()
			.setTrackerClass("io.ghostwriter.ScottRuntime")
			.setInclude(getPropertyConfig("GHOSTWRITER_INCLUDE", new String[] {}))
			.setExclude(getPropertyConfig("GHOSTWRITER_EXCLUDE", new String[] {}))
			.setIncludeByAnnotation(setIfPropertyExists("GHOSTWRITER_ANNOTATED_ONLY", new String[] {"io.ghostwriter.annotation.Include"}))
			.setExcludeByAnnotation(Arrays.asList("io.ghostwriter.annotation.Exclude"))
			.setExcludeMethodsByName(getPropertyConfig("GHOSTWRITER_EXCLUDE_METHODS", new String[] {"toString", "equals", "hashCode", "compareTo"}))
			.setIncludeLambdas(false)
			.setTrackReturn(getPropertyConfig("GHOSTWRITER_TRACE_RETURNING", true))
			.setTrackUnhandledException(getPropertyConfig("GHOSTWRITER_TRACE_ON_ERROR", true))
			.setTrackLocalVariableAssignments(getPropertyConfig("GHOSTWRITER_TRACE_VALUE_CHANGE", true))
			.setTrackLocalVariableIncrements(false)
			.setTrackLocalVariablesAfterEveryMethodCall(false)
			.setTrackFieldAssignments(false)
			.setTrackFieldsAfterEveryMethodCall(false)
			.setMinimumMethodLoc(getPropertyConfig("GHOSTWRITER_SHORT_METHOD_LIMIT", 0))
			.setVerboseLogging("true".equalsIgnoreCase(System.getenv("GHOSTWRITER_VERBOSE")))
			.build();
	}

	private static List<String> getPropertyConfig(String propertyKey, final String[] defaultValues) {
		final String property = System.getProperty(propertyKey);

		final String[] params;
		if (property != null) {
			params = property.split(",");
		} else {
			params = defaultValues;
		}

		return Arrays.asList(params);
	}

	private static List<String> setIfPropertyExists(String propertyKey, final String[] values) {
		if (System.getProperty(propertyKey) != null) {
			return Arrays.asList(values);
		} else {
			return new ArrayList<String>();
		}
	}

	private static boolean getPropertyConfig(String propertyKey, final boolean defaultValue) {
		String propertyValue = System.getProperty(propertyKey);
		if (propertyValue != null) {
			return Boolean.parseBoolean(propertyValue);
		} else {
			return defaultValue;
		}
	}

	private static int getPropertyConfig(String propertyKey, final int defaultValue) {
		String propertyValue = System.getProperty(propertyKey);
		if (propertyValue != null) {
			return Integer.parseInt(propertyValue);
		} else {
			return defaultValue;
		}
	}

}
