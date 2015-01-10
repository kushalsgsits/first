package com.fem.globals;

/**
 * The Class Enums.
 * 
 * @author KushalC
 */
public class Enums
{

	/**
	 * The Class JSONKey has keys to get values from JSON object
	 * 
	 * @author KushalC
	 */
	public static class JSONKey
	{
		// Keys related entity Group
		public static final String GROUP_ID = "group_id";
		public static final String GROUP_NAME = "group_name";
		public static final String GROUP_MEMBERS = "group_members";
		public static final String GROUPS = "groups";

		// Keys related entity Entry
		public static final String AMOUNT = "amount";
		public static final String SHARED_BY = "shared_by";
		public static final String SPENT_BY = "spent_by";
		public static final String SPENT_DATE = "spent_date";
		public static final String ENTRY_DATE = "entry_date";
		public static final String DESCRIPTION = "description";
		public static final String ENTERED_BY = "entered_by";
		public static final String ENTRY_ID = "entry_id";

		// Keys related to entity Member
		public static final String MEMBER_ID = "member_id";
		public static final String MEMBER_NAME = "member_name";
		public static final String MEMBER_BALANCE = "member_balance";
		public static final String MEMBER_CONFIRM = "member_confirm";

		// Key to get JSON content from HTTP request
		public static final String JSON_CONTENT = "json_content";

		// Response JSON keys
		public static final String REQUEST_STATUS = "request_status";
		public static final String RESPONSE_MESSAGE = "response_message";
		public static final String RESPONSE_DATA = "response_data";

		// History related
		public static final String HISTORY_OFFSET = "history_offset";
		public static final String HISTORY_COUNT = "history_count";
		public static final String HISTORY_LIST = "history_list";
	}

	/**
	 * The Class ResponseMessage has messages to be sent as a part of response
	 * 
	 * @author KushalC
	 */
	public static class ResponseMessage
	{
		public static final String ENTRY_ADDED = "Entry was added successfully";
		public static final String ENTRY_NOT_ADDED = "Entry was NOT added successfully";

		public static final String ENTRY_DELETED = "Entry was deleted successfully";
		public static final String ENTRY_NOT_DELETED = "Entry was NOT deleted successfully";

		public static final String GROUP_CREATED = "Group was created successfully";
		public static final String GROUP_NOT_CREATED = "Group was NOT created successfully";

		public static final String GROUP_UPDATED = "Group was updated successfully";
		public static final String GROUP_NOT_UPDATED = "Group was NOT updated successfully";

		public static final String GROUP_LEFT = "Left group successfully";
		public static final String GROUP_NOT_LEFT = "Could not leave group successfully";

		public static final String DASHBOARD_FETCHED = "Dashboard fetched successfully";
		public static final String DASHBOARD_NOT_FETCHED = "Dashboard not fetched successfully";
		
		public static final String HISTORY_FETCHED = "History fetched successfully";
		public static final String HISTORY_NOT_FETCHED = "History not fetched successfully";
	}
}
