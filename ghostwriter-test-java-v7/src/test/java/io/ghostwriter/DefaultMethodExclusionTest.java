package io.ghostwriter;

import java.util.ArrayList;
import java.util.Collections;

import io.ghostwriter.annotation.Exclude;
import io.ghostwriter.test.TestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@Exclude
public class DefaultMethodExclusionTest extends TestBase {

	public class ClassUnderTest implements Comparable<ClassUnderTest> {

		@Override
		public String toString() {
			return "dummy";
		}

		@Override
		public boolean equals(Object o) {
			return false;
		}

		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		public int compareTo(ClassUnderTest classUnderTest) {
			return -1;
		}

		public int dummy() {
			return 0;
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
	public void ToStringIsExcludedByDefault() {
		classUnderTest.toString();
		Assert.assertEquals(Collections.emptyList(), new ArrayList<>(inMemoryTracer.getMessages()));
	}

	@Test
	public void equalsIsExcludedByDefault() {
		classUnderTest.equals(classUnderTest);
		Assert.assertEquals(Collections.emptyList(), new ArrayList<>(inMemoryTracer.getMessages()));
	}

	@Test
	public void hashCodeIsExcludedByDefault() {
		classUnderTest.hashCode();
		Assert.assertEquals(Collections.emptyList(), new ArrayList<>(inMemoryTracer.getMessages()));
	}

	@Test
	public void compareToIsExcludedByDefault() {
		classUnderTest.compareTo(classUnderTest);
		Assert.assertEquals(Collections.emptyList(), new ArrayList<>(inMemoryTracer.getMessages()));
	}

	@Test
	public void dummyIsStillInstrumentedByDefault() {
		classUnderTest.dummy();
		Assert.assertTrue(inMemoryTracer.numberOfMessages() > 0);
	}
}
