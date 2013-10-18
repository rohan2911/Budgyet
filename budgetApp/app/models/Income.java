package models;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Income {

	public BigDecimal amount;
	public List<String> tags;
	public Date income_date;
	public String description;
	
	public Income(String amount, String tags, String date, String description) {
		this.amount = new BigDecimal(amount);
		this.tags = new ArrayList<String>(Arrays.asList(tags.split(",")));
		try {
			this.income_date = new SimpleDateFormat("yyyy-mm-dd").parse(date);
		} catch (ParseException e) {
			this.income_date = null;
			e.printStackTrace();
		}
		this.description = description;
	}
	
	
	public static boolean add(Income income) {
		return false;
		
	}
	
}
