package com.fem.servlets;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.fem.entities.Entry;
import com.fem.entities.Group;
import com.fem.entities.Member;
import com.fem.globals.Enums.JSONKey;
import com.fem.globals.Enums.ResponseMessage;
import com.fem.persistence.EMF;
import com.fem.utils.Utility;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * The Class DeleteEntryServlet.
 */
@SuppressWarnings({ "serial", "unchecked" })
public class DeleteEntryServlet extends HttpServlet
{

	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(DeleteEntryServlet.class.getSimpleName());

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		boolean entryDeleted = false;
		JSONObject requestJSONObj = null;
		for (int i = 0; i < 3; i++)
		{
			try
			{
				requestJSONObj = Utility.getRequestJSON(req);
				this.deleteEntry(requestJSONObj);
				entryDeleted = true;
				logger.info("Entry deleted successfully. Attempt: " + (i + 1));
				break;
			}
			catch (ParseException e)
			{
				logger.severe("Could not parse the request: " + req.getParameter("json_content"));
				logger.severe(e.toString());
				break;
			}
			catch (Exception e)
			{
				logger.severe("Could not delete entry in attempt: " + (i + 1));
			}
		}

		String responseJSON = getResponseJSON(entryDeleted);
		resp.setContentType("text/plain");
		resp.getWriter().println("In EntryServlet:\n" + responseJSON);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		this.doPost(req, resp);
	}

	/**
	 * Delete entry.
	 * 
	 * @param jsonObj
	 *            the json obj
	 * @throws Exception
	 *             the exception
	 */
	public void deleteEntry(JSONObject jsonObj) throws Exception
	{
		String entryID = jsonObj.get(JSONKey.ENTRY_ID).toString();

		EntityManager em = EMF.getInstance().createEntityManager();
		EntityTransaction et = em.getTransaction();
		et.begin();

		try
		{
			// Get the entry to be deleted
			Key entryKey = KeyFactory.createKey(Entry.class.getSimpleName(), entryID);
			Entry entry = em.find(Entry.class, entryKey);
			if (null == entry)
			{
				return;
			}
			em.lock(entry, LockModeType.WRITE);
			Double amount = entry.getAmount();
			List<Key> sharedByKeys = entry.getSharedBy();
			Key spentByKey = entry.getSpentBy();
			Key groupKey = entry.getGroupKey();

			int noOfShares = sharedByKeys.size();
			double perHeadShare = amount / noOfShares;

			// Get the group to be updated
			Group group = em.find(Group.class, groupKey);
			em.lock(group, LockModeType.WRITE);
			Map<Key, Double> memberBalanceMap = group.getMemberBalanceMap();

			logger.info("Deleting entry from group: " + group.getGroupName());
			logger.finest("Total amount spent: " + amount);
			logger.finest("Amount spent by: " + spentByKey);
			logger.finest("Amount shared by:: Size: " + noOfShares + ", List: " + sharedByKeys);
			logger.finest("Per head share: " + perHeadShare);
			logger.finest("Group status before deleting entry: " + memberBalanceMap);

			Member spentByMember = em.find(Member.class, spentByKey);
			if (null == spentByMember)
			{
				// TODO throw Exception
				logger.severe("Invalid spentBy member: " + spentByKey);
				throw new Exception("Invalid spentBy member: " + spentByKey);
			}
			else
			{
				// Decrease the previously added balance of spender by amount. If he/she has share
				// (perHeadShare) in expenditure then it will be added in next block of code
				Double balance = memberBalanceMap.get(spentByKey);
				balance -= amount;
				memberBalanceMap.put(spentByKey, balance);
			}

			for (Key memberKey : sharedByKeys)
			{
				Member member = em.find(Member.class, memberKey);
				if (null == member)
				{
					// TODO throw Exception
					logger.severe("Invalid sharedBy member: " + memberKey);
					throw new Exception("Invalid sharedBy member: " + memberKey);
				}
				else
				{
					// Add the previously deducted balance by perHeadShare
					Double balance = memberBalanceMap.get(memberKey);
					balance += perHeadShare;
					memberBalanceMap.put(memberKey, balance);
				}
			}

			// Delete this entry from database
			em.remove(entry);
			// Store updated group in database
			em.persist(group);

			et.commit();
			logger.info("Group status after deleting entry: " + memberBalanceMap);
		}
		catch (Exception e)
		{
			et.rollback();
			logger.severe(e.getMessage());
			throw e;
		}
		finally
		{
			em.close();
		}
	}

	/**
	 * Gets the response json.
	 * 
	 * @param entryDeleted
	 *            the entry deleted
	 * @return the response json
	 */
	private String getResponseJSON(boolean entryDeleted)
	{
		int requestStatus = entryDeleted ? 1 : 0;
		String responseMessage = entryDeleted ? ResponseMessage.ENTRY_DELETED
				: ResponseMessage.ENTRY_NOT_DELETED;

		JSONObject responseJSONObj = new JSONObject();
		responseJSONObj.put(JSONKey.REQUEST_STATUS, requestStatus);
		responseJSONObj.put(JSONKey.RESPONSE_MESSAGE, responseMessage);
		return responseJSONObj.toJSONString();
	}

}
