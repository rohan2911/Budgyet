import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import models.Income;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class IncomeTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void tagNamesTest() {
		List<String> tagNames = new ArrayList<String>();
		tagNames.add("Tokyo");
		tagNames.add("New York");
		tagNames.add("Sydney");
		String tags = Income.listToString(tagNames);
		assertTrue(tags.equals("Tokyo,New York,Sydney"));
	}
	
	@Test
	public void tagValsTest() {
		List<String> tagVals = new ArrayList<String>();
		tagVals.add("2.50");
		tagVals.add("3.20");
		tagVals.add("1.80");
		String tags = Income.listToString(tagVals);
		assertTrue(tags.equals("2.50,3.20,1.80"));
	}

}
