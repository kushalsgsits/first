package com.fem.servlets;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
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
 * The Class HistoryServlet.
 */
@SuppressWarnings({ "serial", "unchecked" })
public class HistoryServlet extends HttpServlet
{

	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(HistoryServlet.class.getSimpleName());

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		JSONObject requestJSONObj = null;
		JSONObject historyJSONObj = null;

		for (int i = 0; i < 3; i++)
		{
			try
			{
				requestJSONObj = Utility.getRequestJSON(req);
				historyJSONObj = this.fetchGroupHistory(requestJSONObj);
				if (null != historyJSONObj)
				{
					logger.info("Group history fetched succesfully. Attempt: " + (i + 1));
					break;
				}
			}
			catch (ParseException e)
			{
				logger.severe("Could not parse the request: " + req.getParameter("json_content"));
				logger.severe(e.getMessage());
				break;
			}
			catch (Exception e)
			{
				logger.severe("Could not fetch group history in attempt: " + (i + 1));
			}
		}

		String responseJSON = getResponseJSON(historyJSONObj);
		resp.setContentType("text/plain");
		resp.getWriter().println(/* "In DashBoardServlet:\n" + */responseJSON);
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
	 * Fetch group history.
	 * 
	 * @param requestJSONObj
	 *            the request json obj
	 * @return the JSON object
	 * @throws Exception
	 *             the exception
	 */
	public JSONObject fetchGroupHistory(JSONObject requestJSONObj) throws Exception
	{
		EntityManager em = EMF.getInstance().createEntityManager();
		EntityTransaction et = em.getTransaction();
		et.begin();

		try
		{
			JSONArray entryJSONArray = new JSONArray();
			JSONObject historyJSONObj = new JSONObject();
			historyJSONObj.put(JSONKey.HISTORY_LIST, entryJSONArray);

			String groupID = requestJSONObj.get(JSONKey.GROUP_ID).toString();
			Key groupKey = KeyFactory.createKey(Group.class.getSimpleName(), groupID);

			Query query = em.createQuery("select e from " + Entry.class.getSimpleName()
					+ " e where e.groupKey=:groupKey ORDER BY e.entryDate DESC");
			query.setFirstResult(Integer.parseInt(requestJSONObj.get(JSONKey.HISTORY_OFFSET)
					.toString()));
			query.setMaxResults(Integer.parseInt(requestJSONObj.get(JSONKey.HISTORY_COUNT)
					.toString()));
			query.setParameter("groupKey", groupKey);

			logger.info("Fetching history of group: " + groupID);

			List<?> rs = query.getResultList();
			logger.finest("======== History of group (start) ========");
			int i = 0;
			for (Object object : rs)
			{
				Entry entry = (Entry) object;
				JSONObject entryJSONObj = new JSONObject();

				Key spentByKey = entry.getSpentBy();
				List<Key> sharedByKeys = entry.getSharedBy();

				logger.finest("-----Entry ##### " + ++i + "-----");
				logger.finest("Description: " + entry.getDescription());
				logger.finest("Amount: " + entry.getAmount());
				logger.finest("Spent Date: " + entry.getSpentDate());
				logger.finest("Entry Date: " + entry.getEntryDate());
				logger.finest("Spent By: " + spentByKey);
				logger.finest("Shared By: " + sharedByKeys);
				logger.finest("Entry ID:" + entry.getKey().getName());
				logger.finest("-----Entry #####-----");

				Member spentByMember = em.find(Member.class, spentByKey);
				JSONObject spentByJSONObj = new JSONObject();
				spentByJSONObj.put(JSONKey.MEMBER_ID, spentByMember.getKey().getName());
				spentByJSONObj.put(JSONKey.MEMBER_NAME, spentByMember.getName());

				JSONArray sharedByJSONArray = new JSONArray();
				for (Key sharedByKey : sharedByKeys)
				{
					Member sharedByMember = em.find(Member.class, sharedByKey);
					JSONObject sharedByJSONObj = new JSONObject();
					sharedByJSONObj.put(JSONKey.MEMBER_ID, sharedByMember.getKey().getName());
					sharedByJSONObj.put(JSONKey.MEMBER_NAME, sharedByMember.getName());
					sharedByJSONArray.add(sharedByJSONObj);
				}

				SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, ''yy");

				entryJSONObj.put(JSONKey.ENTRY_ID, entry.getKey().getName());
				entryJSONObj.put(JSONKey.AMOUNT, entry.getAmount());
				entryJSONObj.put(JSONKey.SPENT_DATE, dateFormat.format(entry.getSpentDate()));
				entryJSONObj.put(JSONKey.ENTRY_DATE, dateFormat.format(entry.getEntryDate()));
				entryJSONObj.put(JSONKey.DESCRIPTION, entry.getDescription());
				entryJSONObj.put(JSONKey.SPENT_BY, spentByJSONObj);
				entryJSONObj.put(JSONKey.SHARED_BY, sharedByJSONArray);

				JSONObject enteredByJSONObj = new JSONObject();
				enteredByJSONObj.put(JSONKey.MEMBER_ID, entry.getEnteredByKey().getName());
				enteredByJSONObj.put(JSONKey.MEMBER_NAME, entry.getEnteredByName());
				entryJSONObj.put(JSONKey.ENTERED_BY, enteredByJSONObj);

				entryJSONArray.add(entryJSONObj);
			}
			logger.finest("======== History of group (end) ========");
			et.commit();

			logger.info("Fetched history of group: " + groupID);
			return historyJSONObj;
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
	 * @param historyJSONObj
	 *            the history json obj
	 * @return the response json
	 */
	private String getResponseJSON(JSONObject historyJSONObj)
	{
		int requestStatus = historyJSONObj != null ? 1 : 0;
		String responseMessage = historyJSONObj != null ? ResponseMessage.DASHBOARD_FETCHED
				: ResponseMessage.DASHBOARD_NOT_FETCHED;
		JSONObject responseData = historyJSONObj != null ? historyJSONObj : new JSONObject();

		JSONObject responseJSONObj = new JSONObject();
		responseJSONObj.put(JSONKey.REQUEST_STATUS, requestStatus);
		responseJSONObj.put(JSONKey.RESPONSE_MESSAGE, responseMessage);
		responseJSONObj.put(JSONKey.RESPONSE_DATA, responseData);
		return responseJSONObj.toJSONString();
	}
}
