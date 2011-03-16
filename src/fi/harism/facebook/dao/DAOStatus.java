package fi.harism.facebook.dao;

/**
 * Simple storage class for Status messages.
 * 
 * @author harism
 */
public class DAOStatus {

	private String message;

	public DAOStatus(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

}
