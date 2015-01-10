package com.fem.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
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
 * The Class UpdateGroupServlet.
 */
@SuppressWarnings({ "serial", "unchecked" })
public class UpdateGroupServlet extends HttpServlet
{

	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(UpdateGroupServlet.class.getSimpleName());

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		boolean groupUpdated = false;
		JSONObject requestJSONObj = null;
		for (int i = 0; i < 3; i++)
		{
			try
			{
				requestJSONObj = Utility.getRequestJSON(req);
				this.updateGroup(requestJSONObj);
				groupUpdated = true;
				logger.info("Group updated succesfully. Attempt: " + (i + 1));
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
				logger.severe("Could not update group in attempt: " + (i + 1));
			}
		}

		String responseJSON = getResponseJSON(groupUpdated);
		resp.setContentType("text/plain");
		resp.getWriter().println("In CreateGroupServlet:\n" + responseJSON);
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
	 * Update group.
	 * 
	 * @param jsonObj
	 *            the json obj
	 * @throws Exception
	 *             the exception
	 */
	public void updateGroup(JSONObject jsonObj) throws Exception
	{
		String groupID = jsonObj.get(JSONKey.GROUP_ID).toString();
		String groupName = jsonObj.get(JSONKey.GROUP_NAME).toString();
		JSONArray groupMembers = (JSONArray) jsonObj.get(JSONKey.GROUP_MEMBERS);

		EntityManager em = EMF.getInstance().createEntityManager();
		EntityTransaction et = em.getTransaction();
		et.begin();

		try
		{
			Key groupKey = KeyFactory.createKey(Group.class.getSimpleName(), groupID);
			Group group = em.find(Group.class, groupKey);

			if (null == group)
			{
				logger.severe("Group does not exist");
				throw new Exception("Group does not exist");
			}
			em.lock(group, LockModeType.WRITE);

			if (null != groupMembers && groupMembers.size() > 0)
			{
				Map<Key, Double> memberBalanceMap = group.getMemberBalanceMap();
				// Populate members and initial balances of the groups
				for (Object memberObj : groupMembers)
				{
					JSONObject memberJsonObject = (JSONObject) memberObj;
					String emailID = memberJsonObject.get(JSONKey.MEMBER_ID).toString();
					String memberName = memberJsonObject.get(JSONKey.MEMBER_NAME).toString();
					boolean memberConfirmed = Boolean.parseBoolean(memberJsonObject.get(
							JSONKey.MEMBER_CONFIRM).toString());
					logger.finest("Adding member: " + emailID);
					Key memberKey = KeyFactory.createKey(Member.class.getSimpleName(), emailID);
					memberBalanceMap.put(memberKey, 0.0);

					// Check existence of current member in database
					Member member = em.find(Member.class, memberKey);
					if (null == member)
					{
						// This is new application user. Add entry in database.
						member = this.getNewMember(memberKey, memberName, memberConfirmed);
						// TODO Send invitation mail to new member. Include groupName and list of
						// members
						// this.sendInvite(emailID);
					}

					// Add this group to the list of groups of current member.
					member.getGroups().add(groupKey);
					em.persist(member);
				}
				group.setMemberBalanceMap(memberBalanceMap);
			}

			if (null != groupName && !groupName.trim().equals(""))
			{
				group.setGroupName(groupName.trim());
			}
			else
			{
				logger.severe("Group name is blank");
				throw new Exception("Group name is blank");
			}

			em.persist(group);

			et.commit();
			logger.info("Updated group: " + groupName + ". Size: "
					+ group.getMemberBalanceMap().size());
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
	 * Gets the response json.
	 * 
	 * @param groupCreated
	 *            the group created
	 * @return the response json
	 */
	private String getResponseJSON(boolean groupCreated)
	{
		int requestStatus = groupCreated ? 1 : 0;
		String responseMessage = groupCreated ? ResponseMessage.GROUP_UPDATED
				: ResponseMessage.GROUP_NOT_UPDATED;

		JSONObject responseJSONObj = new JSONObject();
		responseJSONObj.put(JSONKey.REQUEST_STATUS, requestStatus);
		responseJSONObj.put(JSONKey.RESPONSE_MESSAGE, responseMessage);
		return responseJSONObj.toJSONString();
	}

}
