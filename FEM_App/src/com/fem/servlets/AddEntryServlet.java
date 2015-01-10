package com.fem.servlets;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
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
 * The Class AddEntryServlet.
 */
@SuppressWarnings({ "serial", "unchecked" })
public class AddEntryServlet extends HttpServlet
{

	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(AddEntryServlet.class.getSimpleName());

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		boolean entryAdded = false;
		JSONObject requestJSONObj = null;
		for (int i = 0; i < 3; i++)
		{
			try
			{
				requestJSONObj = Utility.getRequestJSON(req);
				this.addEntry(requestJSONObj);
				entryAdded = true;
				logger.info("Entry added successfully. Attempt: " + (i + 1));
				break;
			}
			catch (ParseException e)
			{
				logger.severe("Could not parse the request: " + req.getParameter("json_content"));
				logger.severe(e.getMessage());
				break;
			}
			catch (Exception e)
			{
				logger.severe("Could not add entry in attempt: " + (i + 1));
			}
		}

		String responseJSON = this.getResponseJSON(entryAdded);
		resp.setContentType("text/plain");
		resp.getWriter().println(responseJSON);
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
	 * Adds the entry.
	 * 
	 * @param jsonObj
	 *            the json obj
	 * @throws Exception
	 *             the exception
	 */
	public void addEntry(JSONObject jsonObj) throws Exception
	{
		String groupID = jsonObj.get(JSONKey.GROUP_ID).toString();
		String spentBy = jsonObj.get(JSONKey.SPENT_BY).toString();
		JSONArray sharedBy = (JSONArray) jsonObj.get(JSONKey.SHARED_BY);
		Double amount = Double.parseDouble(jsonObj.get(JSONKey.AMOUNT).toString());
		String description = jsonObj.get(JSONKey.DESCRIPTION).toString();
		String spentDateStr = jsonObj.get(JSONKey.SPENT_DATE).toString();
		JSONObject enteredByJSONObj = (JSONObject) jsonObj.get(JSONKey.ENTERED_BY);
		String enteredByEmailID = enteredByJSONObj.get(JSONKey.MEMBER_ID).toString();
		String enteredByName = enteredByJSONObj.get(JSONKey.MEMBER_NAME).toString();

		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		Date spentDate = formatter.parse(spentDateStr);

		EntityManager em = EMF.getInstance().createEntityManager();
		EntityTransaction et = em.getTransaction();
		et.begin();

		try
		{
			int noOfShares = sharedBy.size();
			double perHeadShare = amount / noOfShares;

			Key groupKey = KeyFactory.createKey(Group.class.getSimpleName(), groupID);
			Group group = em.find(Group.class, groupKey);
			em.lock(group, LockModeType.WRITE);
			Map<Key, Double> memberBalanceMap = group.getMemberBalanceMap();

			logger.info("Adding entry for group: " + group.getGroupName());
			logger.finest("Total amount spent: " + amount);
			logger.finest("Amount spent by: " + spentBy);
			logger.finest("Amount shared by:: Size: " + noOfShares + ", List: " + sharedBy);
			logger.finest("Per head share: " + perHeadShare);
			logger.finest("Group status before adding new entry: " + memberBalanceMap);
			logger.finest("Data entered by: " + enteredByName);

			Key spentByKey = KeyFactory.createKey(Member.class.getSimpleName(), spentBy);
			Member spentByMember = em.find(Member.class, spentByKey);
			if (null == spentByMember)
			{
				logger.severe("Invalid spentBy member: " + spentByKey);
				throw new Exception("Invalid spentBy member: " + spentByKey);
			}
			else
			{
				// Increase balance of spender by amount. If he/she has share (perHeadShare) in
				// expenditure then it will be deducted in next block of code
				Double balance = memberBalanceMap.get(spentByKey);
				balance += amount;
				memberBalanceMap.put(spentByKey, balance);
			}

			List<Key> sharedByKeys = new ArrayList<Key>();
			for (Object emailID : sharedBy)
			{
				Key memberKey = KeyFactory.createKey(Member.class.getSimpleName(),
						emailID.toString());
				sharedByKeys.add(memberKey);
				Member member = em.find(Member.class, memberKey);
				if (null == member)
				{
					logger.severe("Invalid sharedBy member: " + memberKey);
					throw new Exception("Invalid sharedBy member: " + memberKey);
				}
				else
				{
					// Deduct balance by perHeadShare
					Double balance = memberBalanceMap.get(memberKey);
					balance -= perHeadShare;
					memberBalanceMap.put(memberKey, balance);
				}
			}

			// Prepare new entry to be persisted in database
			String entryID = UUID.randomUUID().toString();
			Key entryKey = KeyFactory.createKey(Entry.class.getSimpleName(), entryID);
			Entry entry = new Entry();
			entry.setAmount(amount);
			entry.setDescription(description);
			entry.setKey(entryKey);
			entry.setSharedBy(sharedByKeys);
			entry.setSpentBy(spentByKey);
			entry.setSpentDate(spentDate);
			entry.setGroupKey(groupKey);
			Key enteredByKey = KeyFactory.createKey(Member.class.getSimpleName(), enteredByEmailID);
			entry.setEnteredBy(enteredByKey);
			entry.setEnteredByName(enteredByName);

			// Store new entry in database
			em.persist(entry);
			// Store updated group in database
			em.persist(group);

			et.commit();
			logger.info("Group status after adding new entry: " + memberBalanceMap);
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
	 * @param entryAdded
	 *            the entry added
	 * @return the response json
	 */
	private String getResponseJSON(boolean entryAdded)
	{
		int requestStatus = entryAdded ? 1 : 0;
		String responseMessage = entryAdded ? ResponseMessage.ENTRY_ADDED
				: ResponseMessage.ENTRY_NOT_ADDED;

		JSONObject responseJSONObj = new JSONObject();
		responseJSONObj.put(JSONKey.REQUEST_STATUS, requestStatus);
		responseJSONObj.put(JSONKey.RESPONSE_MESSAGE, responseMessage);
		return responseJSONObj.toJSONString();
	}

}
