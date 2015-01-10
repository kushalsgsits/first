package com.fem.entities;

import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import com.google.appengine.api.datastore.Key;

@Entity
public class Group
{
	@Id
	private Key key;
	private String groupName;
	Map<Key, Double> memberBalanceMap;
	@Version
	long version;

	public Key getKey()
	{
		return key;
	}

	public void setKey(Key key)
	{
		this.key = key;
	}

	public String getGroupName()
	{
		return groupName;
	}

	public void setGroupName(String groupName)
	{
		this.groupName = groupName;
	}

	public Map<Key, Double> getMemberBalanceMap()
	{
		return memberBalanceMap;
	}

	public void setMemberBalanceMap(Map<Key, Double> memberBalanceMap)
	{
		this.memberBalanceMap = memberBalanceMap;
	}

	public Group()
	{
	}

}
