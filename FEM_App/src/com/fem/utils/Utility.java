package com.fem.utils;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Utility
{
	private static final Logger logger = Logger.getLogger(Utility.class.getSimpleName());
	
	public static JSONObject getRequestJSON(HttpServletRequest req) throws ParseException
	{
		String jsonStr = req.getParameter("json_content");
		JSONParser parser = new JSONParser();
		JSONObject jsonObj = (JSONObject) parser.parse(jsonStr);
		logger.info("Request jsonObj: " + jsonObj);
		return jsonObj;
	}
}
