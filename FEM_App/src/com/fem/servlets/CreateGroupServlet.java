package com.fem.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

import com.fem.entities.Group;
import com.fem.entities.Member;
import com.fem.globals.Enums.JSONKey;
import com.fem.globals.Enums.ResponseMessage;
import com.fem.persistence.EMF;
import com.fem.utils.Mail;
import com.fem.utils.Utility;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * The Class CreateGroupServlet.
 */
@SuppressWarnings({ "serial", "unchecked" })
public class CreateGroupServlet extends HttpServlet
{

	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(CreateGroupServlet.class.getSimpleName());

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		JSONObject groupJSONObj = null;
		JSONObject requestJSONObj = null;
		for (int i = 0; i < 3; i++)
		{
			try
			{
				requestJSONObj = Utility.getRequestJSON(req);
				groupJSONObj = this.createGroup(requestJSONObj);
				logger.info("Group created succesfully. Attempt: " + (i + 1));
				break;
			}
			catch (ParseException e)
			{
				logger.severe("Failed to parse the request: " + req.getParameter("json_content"));
				logger.severe(e.getMessage());
				break;
			}
			catch (Exception e)
			{
				logger.severe("Could not create group in attempt: " + (i + 1));
			}
		}

		String responseJSON = getResponseJSON(groupJSONObj, requestJSONObj);
		resp.setContentType("text/plain");
		resp.getWriter().println(/* "In CreateGroupServlet:\n" + */responseJSON);
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
	 * Creates the group.
	 * 
	 * @param jsonObj
	 *            the json obj
	 * @return the JSON object
	 * @throws Exception
	 *             the exception
	 */
	public JSONObject createGroup(JSONObject jsonObj) throws Exception
	{
		Object groupNameObj = jsonObj.get(JSONKey.GROUP_NAME);
		String groupName = null == groupNameObj ? null : groupNameObj.toString().trim();
		Object groupMembersObj = jsonObj.get(JSONKey.GROUP_MEMBERS);
		JSONArray groupMembers = null == groupMembersObj ? null : (JSONArray) groupMembersObj;

		EntityManager groupEM = EMF.getInstance().createEntityManager();
		EntityTransaction groupET = groupEM.getTransaction();
		groupET.begin();

		EntityManager memberEM = EMF.getInstanceMultipleEntities().createEntityManager();
		EntityTransaction memberET = memberEM.getTransaction();
		memberET.begin();

		try
		{
			if (null == groupMembers || groupMembers.size() < 2)
			{
				logger.severe("Could not create group. "
						+ "Atleast 2 members are required to create group");
				throw new Exception("Could not create group. "
						+ "Atleast 2 members are required to create group");
			}
			if (null == groupName || groupName.equals(""))
			{
				logger.severe("Could not create group. Group name is blank");
				throw new Exception("Could not create group. Group name is blank");
			}

			logger.info("Creating new group: " + groupName);
			Map<Key, Double> memberBalanceMap = new HashMap<Key, Double>();

			// Create unique group ID and key for database
			String groupID = UUID.randomUUID().toString();
			Key groupKey = KeyFactory.createKey(Group.class.getSimpleName(), groupID);

			// Populate members and initial balances of the groups
			for (Object memberObj : groupMembers)
			{
				JSONObject memberJSONObj = (JSONObject) memberObj;
				String emailID = memberJSONObj.get(JSONKey.MEMBER_ID).toString();
				String memberName = memberJSONObj.get(JSONKey.MEMBER_NAME).toString();
				boolean memberConfirmed = Boolean.parseBoolean(memberJSONObj.get(
						JSONKey.MEMBER_CONFIRM).toString());

				logger.finest("Adding member: " + emailID + " in group: " + groupName);
				Key memberKey = KeyFactory.createKey(Member.class.getSimpleName(), emailID);
				memberBalanceMap.put(memberKey, 0.0);

				// Check existence of current member in database
				Member member = memberEM.find(Member.class, memberKey);
				if (null == member)
				{
					// This is new app user. Add entry in database.
					member = this.getNewMember(memberKey, memberName, memberConfirmed);
					// TODO Send invitation mail to new member. Include groupName and list of
					// members
					// this.sendInvite(emailID);
				}
				else
				{
					memberEM.lock(member, LockModeType.WRITE);
					if (!member.isMemberConfirmed() && memberConfirmed)
					{
						member.setName(memberName);
						member.setMemberConfirmed(memberConfirmed);
					}
				}

				// Add this group to the list of groups of current member.
				member.getGroups().add(groupKey);
				memberEM.persist(member);
			}

			memberET.commit();

			Group group = new Group();
			group.setKey(groupKey);
			group.setGroupName(groupName);
			group.setMemberBalanceMap(memberBalanceMap);
			groupEM.persist(group);

			groupET.commit();

			logger.info("Created new group: " + groupName + ". Size: "
					+ group.getMemberBalanceMap().size());

			JSONObject groupJSONObj = new JSONObject();
			groupJSONObj.put(JSONKey.GROUP_ID, groupID);
			groupJSONObj.put(JSONKey.GROUP_NAME, groupName);
			return groupJSONObj;
		}
		catch (Exception e)
		{
			memberET.rollback();
			groupET.rollback();
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
		finally
		{
			memberEM.close();
			groupEM.close();
		}
	}

	/**
	 * Send invite.
	 * 
	 * @param sendMailTo
	 *            the send mail to
	 * @param sendMailFrom
	 *            the send mail from
	 * @throws Exception
	 *             the exception
	 */
	private void sendInvite(String sendMailTo, String sendMailFrom) throws Exception
	{
		Mail.sendMail(sendMailFrom, sendMailTo, sendMailTo, "FEM: You have been added to group.",
				"FEM: You have been added to group.");
		logger.info("\t-Invite sent to: " + sendMailTo);
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
		logger.info("\t-Adding NEW member: " + key.getName() + " to this application");
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
	 * @param groupJSONObj
	 *            the group json obj
	 * @param requestJSONObj
	 *            the request json obj
	 * @return the response json
	 */
	private String getResponseJSON(JSONObject groupJSONObj, JSONObject requestJSONObj)
	{
		int requestStatus = null != groupJSONObj ? 1 : 0;
		String responseMessage = null != groupJSONObj ? ResponseMessage.GROUP_CREATED
				: ResponseMessage.GROUP_NOT_CREATED;
		JSONObject responseData = null != groupJSONObj ? groupJSONObj : new JSONObject();

		JSONObject responseJSONObj = new JSONObject();
		responseJSONObj.put(JSONKey.REQUEST_STATUS, requestStatus);
		responseJSONObj.put(JSONKey.RESPONSE_MESSAGE, responseMessage);
		responseJSONObj.put(JSONKey.RESPONSE_DATA, responseData);

		return responseJSONObj.toJSONString();
	}
}
