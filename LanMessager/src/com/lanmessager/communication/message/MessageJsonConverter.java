package com.lanmessager.communication.message;

import java.lang.reflect.Field;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class MessageJsonConverter {
	private static final Logger LOGGER = Logger.getLogger(MessageJsonConverter.class.getSimpleName());

	public static String toJson(Message message) {
		JSONObject json = new JSONObject();

		Class<?> clazz = message.getClass();
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			MessageKey messageKey = field.getDeclaredAnnotation(MessageKey.class);
			if (messageKey != null) {
				field.setAccessible(true);
				String key = messageKey.value();
				if (key != null && !key.isEmpty()) {
					try {
						Object value = field.get(message);
						json.put(key, value);
					} catch (IllegalArgumentException | IllegalAccessException ex) {
						LOGGER.error("Failed to get field " + key + " from message.", ex);
					} catch (JSONException ex) {
						LOGGER.error("Failed to put " + key + " value to JSON", ex);
					}
				}
			}
		}

		return json.toString();
	}

	public static Message parseJson(String jsonString, Class<? extends Message> clazz) {
		JSONObject json = null;
		try {
			json = new JSONObject(jsonString);
		} catch (JSONException ex) {
			LOGGER.error("Failed to parse JSON string.", ex);
		}
		if (json == null) {
			return null;
		}
		
		Message message = null;
		try {
			message = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException ex) {
			LOGGER.error("Failed to create new instance.", ex);
		}

		if (message == null) {
			return null;
		}
		
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			MessageKey messageKey = field.getDeclaredAnnotation(MessageKey.class);
			if (messageKey != null) {
				field.setAccessible(true);
				String key = messageKey.value();
				if (key != null && !key.isEmpty()) {
					Object value = null;
					try {
						value = json.get(key);
						field.set(message, value);
					} catch (JSONException ex) {
						LOGGER.error("Failed to get " + key + " value from JSON.", ex);
					} catch (IllegalArgumentException | IllegalAccessException ex) {
						LOGGER.error("Failed to set field " + key + " to message.", ex);
					}
				}
			}
		}

		return message;
	}
}
