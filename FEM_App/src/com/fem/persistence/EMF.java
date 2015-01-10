package com.fem.persistence;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public final class EMF
{
	private static EntityManagerFactory emfInstance = null;

	public static EntityManagerFactory getInstance()
	{
		if (null == emfInstance)
		{
			emfInstance = Persistence.createEntityManagerFactory("fem");
		}
		return emfInstance;
	}
	
	public static EntityManagerFactory getInstanceMultipleEntities()
	{
		EntityManagerFactory emfInstance = Persistence.createEntityManagerFactory("fem");
		return emfInstance;
	}
}
