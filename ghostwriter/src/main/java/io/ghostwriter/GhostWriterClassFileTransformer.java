package io.ghostwriter;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import hu.advancedweb.scott.instrumentation.transformation.ScottClassTransformer;


public class GhostWriterClassFileTransformer {

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			throw new IllegalArgumentException("Exactly one argument is required.");
		}
		if (System.getProperty("GHOSTWRITER_INSTRUMENT") != null && !("true".equals(System.getProperty("GHOSTWRITER_INSTRUMENT")))) {
			log("GhostWriter instrumentation is disabled.");
			return;
		}
		final String rootPath = args[0];
		final PathMatcher classMatcher = FileSystems.getDefault().getPathMatcher("glob:**.class");

		Files.walkFileTree(Paths.get(rootPath), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
				if (attrs.isRegularFile() && classMatcher.matches(path)) {
					transformClass(path);
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private static void transformClass(Path path) {
		try {
			log("GhostWriter instrumenting: " + path);
			byte[] originalClass = Files.readAllBytes(path);
			byte[] instrumentedClass = new ScottClassTransformer().transform(originalClass, GhostWriterConfigurer.getConfiguration());
			Files.write(path, instrumentedClass);
		} catch (Exception e) {
			System.err.println("GhostWriter Could not instrument: " + path);
			e.printStackTrace();
		}
	}

	private static void log(String message) {
		if ("true".equalsIgnoreCase(System.getenv("GHOSTWRITER_VERBOSE"))) {
			System.out.println(message);
		}
	}

}
