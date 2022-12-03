package edu.harvard.iq.dataverse.authorization;

/**
 * A result of an authentication attempt. May succeed, fail, or be in error.
 * Client code may use normal constructors, or use one of the static convenience 
 * methods ({@code createXXX}).
 * 
 * @author michael
 */
public class LdapUsachResponse {

    Boolean success;
    String message;
    UserUsach data;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public UserUsach getData() {
        return data;
    }

    public void setData(UserUsach data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
