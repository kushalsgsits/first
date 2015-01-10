package com.fem.utils;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * The Class Mail.
 */
public class Mail
{

	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(Mail.class.getSimpleName());

	/**
	 * Send mail.
	 * 
	 * @param sendEmailFrom
	 *            the send email from
	 * @param sendMailTo
	 *            the send mail to
	 * @param recipientName
	 *            the recipient name
	 * @param messageSubject
	 *            the message subject
	 * @param messageText
	 *            the message text
	 * @throws Exception
	 *             the exception
	 */
	public static void sendMail(String sendEmailFrom, String sendMailTo, String recipientName,
			String messageSubject, String messageText) throws Exception
	{
		Properties prop = new Properties();
		Session session = Session.getDefaultInstance(prop, null);
		try
		{
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(sendEmailFrom));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(sendMailTo, "Mr./Ms. "
					+ recipientName));
			msg.setSubject(messageSubject);
			msg.setText(messageText);
			Transport.send(msg);
		}
		catch (AddressException e)
		{
			logger.severe("Could not send invite to NEW member");
			logger.severe(e.toString());
			throw e;
		}
		catch (MessagingException e)
		{
			logger.severe("Could not send invite to NEW member");
			logger.severe(e.toString());
			throw e;
		}
		catch (UnsupportedEncodingException e)
		{
			logger.severe("Could not send invite to NEW member");
			logger.severe(e.toString());
			throw e;
		}
		catch (Exception e)
		{
			logger.severe("Could not send invite to NEW member");
			logger.severe(e.toString());
			throw e;
		}
	}
}
