package com.fem.entities;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.google.appengine.api.datastore.Key;

@Entity
public class Entry
{
	@Id
	private Key key;
	@Temporal(TemporalType.DATE)
	private Date spentDate;
	@Temporal(TemporalType.TIMESTAMP)
	private Date entryDate = new Date();
	private Key spentBy;
	private List<Key> sharedBy;
	private Double amount;
	private String description;
	private Key groupKey;
	private Key enteredByKey;
	private String enteredByName;

	public Key getKey()
	{
		return key;
	}

	public void setKey(Key key)
	{
		this.key = key;
	}

	public Date getSpentDate()
	{
		return spentDate;
	}

	public void setSpentDate(Date spentDate)
	{
		this.spentDate = spentDate;
	}

	public Date getEntryDate()
	{
		return entryDate;
	}

	public void setEntryDate(Date entryDate)
	{
		this.entryDate = entryDate;
	}

	public Key getSpentBy()
	{
		return spentBy;
	}

	public void setSpentBy(Key spentBy)
	{
		this.spentBy = spentBy;
	}

	public List<Key> getSharedBy()
	{
		return sharedBy;
	}

	public void setSharedBy(List<Key> sharedBy)
	{
		this.sharedBy = sharedBy;
	}

	public Double getAmount()
	{
		return amount;
	}

	public void setAmount(Double amount)
	{
		this.amount = amount;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public Key getGroupKey()
	{
		return groupKey;
	}

	public void setGroupKey(Key groupKey)
	{
		this.groupKey = groupKey;
	}

	public Key getEnteredByKey()
	{
		return enteredByKey;
	}

	public void setEnteredBy(Key enteredBy)
	{
		this.enteredByKey = enteredBy;
	}

	public String getEnteredByName()
	{
		return enteredByName;
	}

	public void setEnteredByName(String enteredByName)
	{
		this.enteredByName = enteredByName;
	}

	public Entry()
	{
	}

}
