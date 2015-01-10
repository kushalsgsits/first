package com.fem.entities;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import com.google.appengine.api.datastore.Key;

@Entity
public class Member
{
	@Id
	private Key key;
	private String name;
	private List<Key> groups;
	private boolean memberConfirmed;
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

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public List<Key> getGroups()
	{
		return groups;
	}

	public void setGroups(List<Key> groups)
	{
		this.groups = groups;
	}

	public boolean isMemberConfirmed()
	{
		return memberConfirmed;
	}

	public void setMemberConfirmed(boolean memberConfirmed)
	{
		this.memberConfirmed = memberConfirmed;
	}

	public Member()
	{
	}
	
	@Override
	public String toString()
	{
		return "Member(key: " + this.key + ", name: " + this.name + ", groups: " + this.groups
				+ ", memberConfirmed: " + this.memberConfirmed + ")";
	}

}
