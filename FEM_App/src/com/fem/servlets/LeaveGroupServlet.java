package com.fem.servlets;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.fem.entities.Group;
import com.fem.entities.Member;
import com.fem.globals.Enums.JSONKey;
import com.fem.globals.Enums.ResponseMessage;
import com.fem.persistence.EMF;
import com.fem.utils.Utility;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * The Class LeaveGroupServlet.
 */
@SuppressWarnings({ "serial", "unchecked" })
public class LeaveGroupServlet extends HttpServlet
{

	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(LeaveGroupServlet.class.getSimpleName());

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		boolean groupLeft = false;
		JSONObject requestJSONObj = null;
		for (int i = 0; i < 3; i++)
		{
			try
			{
				requestJSONObj = Utility.getRequestJSON(req);
				groupLeft = this.leaveGroup(requestJSONObj);
				groupLeft = true;
				logger.info("Group left succesfully. Attempt: " + (i + 1));
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
				logger.severe("Could not leave group in attempt: " + (i + 1));
			}
		}

		String responseJSON = getResponseJSON(groupLeft);
		resp.setContentType("text/plain");
		resp.getWriter().println("In LeaveGroupServlet:\n" + responseJSON);
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
	 * Leave group.
	 * 
	 * @param jsonObj
	 *            the json obj
	 * @return true, if successful
	 * @throws Exception
	 *             the exception
	 */
	public boolean leaveGroup(JSONObject jsonObj) throws Exception
	{
		String groupID = jsonObj.get(JSONKey.GROUP_ID).toString();
		String emailID = jsonObj.get(JSONKey.MEMBER_ID).toString();

		EntityManager em = EMF.getInstance().createEntityManager();
		EntityTransaction et = em.getTransaction();
		et.begin();

		try
		{
			Key groupKey = KeyFactory.createKey(Group.class.getSimpleName(), groupID);
			em.lock(Group.class, LockModeType.WRITE);
			Group group = em.find(Group.class, groupKey);
			Map<Key, Double> memberBalanceMap = group.getMemberBalanceMap();

			logger.info(emailID + " trying to leave the group: " + group.getGroupName());
			logger.finest("Group status before removing member: " + memberBalanceMap);

			Key leavingMemberKey = KeyFactory.createKey(Member.class.getSimpleName(), emailID);
			if (memberBalanceMap.get(leavingMemberKey) == 0.0)
			{
				// Remove member from group
				memberBalanceMap.remove(leavingMemberKey);
			}
			else
			{
				logger.severe("Leaving member " + emailID
						+ " does not have zero(0) balance. Ensure 0 balance to leave group");
				throw new Exception("Leaving member " + emailID
						+ " does not have zero(0) balance. Ensure 0 balance to leave group");
			}

			// Remove the group from the list of groups of leaving member
			Member leavingMember = em.find(Member.class, leavingMemberKey);
			leavingMember.getGroups().remove(groupKey);

			// Delete or update group
			if (memberBalanceMap.size() == 0)
			{
				// If there are no members in the group then delete the group from the database
				em.remove(group);
				logger.info("Deleting group: " + group.getGroupName()
						+ " as there are no members in the group");
			}
			else
			{
				// Else, store updated group in database
				em.persist(group);
				logger.finest("Group status after removing member: " + memberBalanceMap);
			}

			// Store updated leavingMember in data base
			em.persist(leavingMember);

			et.commit();
			return true;
		}
		catch (RollbackException e)
		{
			if (e.getCause() instanceof OptimisticLockException)
			{
				System.out
						.println("This group was being updated by another user. Please try again.");
				return false;
			}
			else
			{
				e.printStackTrace();
				throw e;
			}
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
	 * @param groupLeft
	 *            the group left
	 * @return the response json
	 */
	private String getResponseJSON(boolean groupLeft)
	{
		int requestStatus = groupLeft ? 1 : 0;
		String responseMessage = groupLeft ? ResponseMessage.GROUP_LEFT
				: ResponseMessage.GROUP_NOT_LEFT;

		JSONObject responseJSONObj = new JSONObject();
		responseJSONObj.put(JSONKey.REQUEST_STATUS, requestStatus);
		responseJSONObj.put(JSONKey.RESPONSE_MESSAGE, responseMessage);
		return responseJSONObj.toJSONString();
	}
}
