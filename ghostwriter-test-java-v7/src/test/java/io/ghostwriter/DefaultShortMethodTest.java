package io.ghostwriter;

import io.ghostwriter.annotation.Exclude;
import io.ghostwriter.test.TestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@Exclude
public class DefaultShortMethodTest extends TestBase {

	public class ClassUnderTest {

		public void shortMethod() {
			// shortest method ever
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
	public void shortMethodLimitIsNotEnabledByDefault() {
		classUnderTest.shortMethod();
		Assert.assertTrue(inMemoryTracer.numberOfMessages() > 0);
	}
}
