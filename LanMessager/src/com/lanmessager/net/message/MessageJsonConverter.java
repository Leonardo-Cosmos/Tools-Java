package com.lanmessager.net.message;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import org.apache.log4j.Logger;
import org.json.JSONArray;
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
					Class<?> fieldType = field.getType();
					if (fieldType.isArray()) {
						toJsonArray(message, json, field, key);	
					} else {
						toJsonValue(message, json, field, key);
					}
				}
			}
		}

		return json.toString();
	}

	private static void toJsonValue(Message message, JSONObject json, Field field, String key) {
		try {
			Object value = field.get(message);
			json.put(key, value);
		} catch (IllegalArgumentException | IllegalAccessException ex) {
			LOGGER.error("Failed to get field " + key + " from message.", ex);
		} catch (JSONException ex) {
			LOGGER.error("Failed to put " + key + " value to JSON", ex);
		}
	}

	private static void toJsonArray(Message message, JSONObject json, Field field, String key) {
		try {
			Object array = field.get(message);
			JSONArray jsonArray = new JSONArray();
			for (int i = 0; i < Array.getLength(array); i++) {
				jsonArray.put(Array.get(array, i));
			}
			json.put(key, jsonArray);
		} catch (IllegalArgumentException | IllegalAccessException ex) {
			LOGGER.error("Failed to get field " + key + " from message.", ex);
		} catch (JSONException ex) {
			LOGGER.error("Failed to put " + key + " value to JSON", ex);
		}
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
					Class<?> fieldType = field.getType();
					if (fieldType.isArray()) {
						parseJsonArray(json, message, field, key);	
					} else {
						parseJsonValue(json, message, field, key);	
					}
				}
			}
		}

		return message;
	}

	private static void parseJsonValue(JSONObject json, Message message, Field field, String key) {
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

	private static void parseJsonArray(JSONObject json, Message message, Field field, String key) {
		Object array = null;
		try {
			JSONArray jsonArray = json.getJSONArray(key);
			array = Array.newInstance(field.getType().getComponentType(), jsonArray.length());
			for (int i = 0; i < jsonArray.length(); i++) {
				Array.set(array, i, jsonArray.get(i));
			}
			field.set(message, array);
		} catch (JSONException ex) {
			LOGGER.error("Failed to get " + key + " value from JSON.", ex);
		} catch (IllegalArgumentException | IllegalAccessException ex) {
			LOGGER.error("Failed to set field " + key + " to message.", ex);
		}
	}
}
