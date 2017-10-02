package io.ghostwriter;

import java.util.ArrayList;
import java.util.Collections;

import io.ghostwriter.annotation.Exclude;
import io.ghostwriter.test.TestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@Exclude
public class EmptyMethodExclusionTest extends TestBase {

	public class ClassUnderTest {

		@Override
		public String toString() {
			return "dummy";
		}
	}

	private InMemoryTracer inMemoryTracer;
	private ClassUnderTest classUnderTest;

	@Before
	public void prepare() {
		inMemoryTracer = fetchedPreparedInMemoryTracer();
		classUnderTest = new ClassUnderTest();
		inMemoryTracer.clearMessages();
	}

	@Test
	public void emptyExclusionOverwriteIsWorking() {
		classUnderTest.toString();
		Assert.assertTrue(inMemoryTracer.numberOfMessages() > 0);
	}
}
