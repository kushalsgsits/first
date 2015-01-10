package com.fem.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
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
 * The Class DashBoardServlet.
 */
@SuppressWarnings({ "serial", "unchecked" })
public class DashBoardServlet extends HttpServlet
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
		JSONObject requestJSONObj = null;
		JSONObject dashboardJSONObj = null;

		for (int i = 0; i < 3; i++)
		{
			try
			{
				requestJSONObj = Utility.getRequestJSON(req);
				dashboardJSONObj = this.fetchDashBoard(requestJSONObj);
				if (null != dashboardJSONObj)
				{
					logger.info("Dashboard fetched succesfully. Attempt: " + (i + 1));
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
				logger.severe("Could not fetch dashboard in attempt: " + (i + 1));
			}
		}

		String responseJSON = getResponseJSON(dashboardJSONObj);
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
	 * Fetch dash board.
	 * 
	 * @param jsonObj
	 *            the json obj
	 * @return the JSON object
	 * @throws Exception
	 *             the exception
	 */
	public JSONObject fetchDashBoard(JSONObject jsonObj) throws Exception
	{
		String emailID = jsonObj.get(JSONKey.MEMBER_ID).toString();

		EntityManager em = EMF.getInstance().createEntityManager();
		EntityTransaction et = em.getTransaction();

		try
		{
			JSONArray groupsJSONArray = new JSONArray();

			Key memberKey = KeyFactory.createKey(Member.class.getSimpleName(), emailID);
			Member member = em.find(Member.class, memberKey);
			if (null == member)
			{
				et.begin();
				String memberName = jsonObj.get(JSONKey.MEMBER_NAME).toString();
				member = this.getNewMember(memberKey, memberName, true);
				em.persist(member);
				et.commit();
			}

			logger.info("Fetching dashboard for member: " + member.getName() + " ("
					+ member.getKey() + ")");
			em.refresh(member);
			List<Key> groups = member.getGroups();
			for (Key groupKey : groups)
			{
				Group group = em.find(Group.class, groupKey);
				if (null == group)
				{
					continue;
				}
				logger.info("Group: " + group.getGroupName() + ", Status: "
						+ group.getMemberBalanceMap());

				JSONArray groupMembersJSONArray = new JSONArray();
				for (Key groupMemberKey : group.getMemberBalanceMap().keySet())
				{
					Member groupMember = em.find(Member.class, groupMemberKey);

					JSONObject memberJSONObj = new JSONObject();
					memberJSONObj.put(JSONKey.MEMBER_ID, groupMember.getKey().getName());
					memberJSONObj.put(JSONKey.MEMBER_NAME, groupMember.getName());
					memberJSONObj.put(JSONKey.MEMBER_BALANCE,
							group.getMemberBalanceMap().get(groupMemberKey));

					groupMembersJSONArray.add(memberJSONObj);
				}

				JSONObject groupJsonObj = new JSONObject();
				groupJsonObj.put(JSONKey.GROUP_ID, group.getKey().getName());
				groupJsonObj.put(JSONKey.GROUP_NAME, group.getGroupName());
				groupJsonObj.put(JSONKey.GROUP_MEMBERS, groupMembersJSONArray);

				groupsJSONArray.add(groupJsonObj);
			}

			JSONObject dashboardJSONObj = new JSONObject();
			dashboardJSONObj.put(JSONKey.GROUPS, groupsJSONArray);

			logger.info("Dashboard fetched for member: " + member.getName() + " ("
					+ member.getKey() + ")");
			return dashboardJSONObj;
		}
		catch (Exception e)
		{
			logger.severe(e.getMessage());
			throw e;
		}
		finally
		{
			em.close();
		}
	}

	/**
	 * Gets the new member.
	 * 
	 * @param key
	 *            the key
	 * @param memberName
	 *            the member name
	 * @param memberConfirmed
	 *            the member confirmed
	 * @return the new member
	 */
	private Member getNewMember(Key key, String memberName, boolean memberConfirmed)
	{
		logger.info("\t" + key.getName()
				+ " has downloded this app. Adding him/her to this application.");
		Member member = new Member();
		member.setKey(key);
		member.setGroups(new ArrayList<Key>());
		member.setName(memberName);
		member.setMemberConfirmed(memberConfirmed);

		return member;
	}

	/**
	 * Gets the response json.
	 * 
	 * @param dashboardJSONObj
	 *            the dashboard json obj
	 * @return the response json
	 */
	private String getResponseJSON(JSONObject dashboardJSONObj)
	{
		int requestStatus = dashboardJSONObj != null ? 1 : 0;
		String responseMessage = dashboardJSONObj != null ? ResponseMessage.DASHBOARD_FETCHED
				: ResponseMessage.DASHBOARD_NOT_FETCHED;
		JSONObject responseData = dashboardJSONObj != null ? dashboardJSONObj : new JSONObject();

		JSONObject responseJSONObj = new JSONObject();
		responseJSONObj.put(JSONKey.REQUEST_STATUS, requestStatus);
		responseJSONObj.put(JSONKey.RESPONSE_MESSAGE, responseMessage);
		responseJSONObj.put(JSONKey.RESPONSE_DATA, responseData);
		return responseJSONObj.toJSONString();
	}
}
