package com.lanmessager.communication.message;

@Deprecated
public class NotifyMessage {
	private int type;
	
	
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	
	public NotifyMessage() {
		
	}
	/*
	public NotifyMessage(String jsonString) {
		try {
			JSONObject json = new JSONObject(jsonString);

			setType(json.getInt("type"));
			setName(json.getString("name"));
			setAddress(json.getString("address"));
			
		} catch (JSONException ex) {
		}
	}
	
	public String toJsonString() {
		JSONObject json = new JSONObject();
		try {
			json.put("type", getType());
			json.put("name", getName());
			json.put("address", getAddress());
		} catch (JSONException ex) {
			
		}
		
		return json.toString();
	}*/
}
