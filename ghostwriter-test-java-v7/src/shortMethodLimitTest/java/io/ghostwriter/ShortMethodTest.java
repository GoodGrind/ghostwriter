package io.ghostwriter;

import java.util.ArrayList;
import java.util.Collections;

import io.ghostwriter.annotation.Exclude;
import io.ghostwriter.test.TestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@Exclude
public class ShortMethodTest extends TestBase {

	// assuming method limit is set to 1
	public class ClassUnderTest {

		public void veryShortMethod() {
			//
		}

		public int shortMethod() {
			return 0;
		}

		public int longerMethod() {
			int result = 1;
			return result;
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
	public void shortMethodIsExcludedIfUnderLimit() {
		classUnderTest.veryShortMethod();
		Assert.assertEquals(Collections.emptyList(), new ArrayList<>(inMemoryTracer.getMessages()));
	}

	@Test
	public void shortMethodIsExcludedIfEqualsLimit() {
		classUnderTest.shortMethod();
		Assert.assertEquals(Collections.emptyList(), new ArrayList<>(inMemoryTracer.getMessages()));
	}

	@Test
	public void shortMethodIsNotExcludedIfAboveLimit() {
		classUnderTest.longerMethod();
		Assert.assertTrue(inMemoryTracer.numberOfMessages() > 0);
	}
}
