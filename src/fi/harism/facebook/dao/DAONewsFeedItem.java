package fi.harism.facebook.dao;

public class DAONewsFeedItem {
	
	private String id;
	private String type;
	private String fromId;
	private String fromName;
	private String message;
	private String picture;
	private String name;
	private String description;
	private String createdTime;
	
	public DAONewsFeedItem(String id, String type, String fromId, String fromName, String message, String picture, String name, String description, String createdTime) {
		this.id = id;
		this.type = type;
		this.fromId = fromId;
		this.fromName = fromName;
		this.message = message;
		this.picture = picture;
		this.name = name;
		this.description = description;
		this.createdTime = createdTime;
	}
	
	public String getId() {
		return id;
	}
	
	public String getType() {
		return type;
	}
	
	public String getFromId() {
		return fromId;
	}
	
	public String getFromName() {
		return fromName;
	}
	
	public String getMessage() {
		return message;
	}
	
	public String getPicture() {
		return picture;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getCreatedTime() {
		return createdTime;
	}

}
