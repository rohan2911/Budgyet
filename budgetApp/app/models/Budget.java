package models;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class Budget {

	public long owner;
	public BigDecimal amount;
	public List<String> tags;
	public Date date_occur;
	public String description;
	
}
