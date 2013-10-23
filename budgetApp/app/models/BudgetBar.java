package models;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Class to store info for budget progress bar.
 * @author Hana
 *
 */
public class BudgetBar {
	
	private Long id;
	private String title;
	private BigDecimal amount;
	private BigDecimal progress;
	private String dateStart;
	private String dateEnd;
	
	public BudgetBar() {
	}
	
	public Long getId() {
		return id;
	}

	public void setId(long l) {
		this.id = l;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getProgress() {
		return progress;
	}

	public void setProgress(BigDecimal progress) {
		this.progress = progress;
	}

	public String getDateEnd() {
		return dateEnd;
	}

	public void setDateEnd(String dateEnd) {
		this.dateEnd = dateEnd;
	}

	public String getDateStart() {
		return dateStart;
	}

	public void setDateStart(String dateStart) {
		this.dateStart = dateStart;
	}
	
	
}
