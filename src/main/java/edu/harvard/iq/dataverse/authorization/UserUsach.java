package edu.harvard.iq.dataverse.authorization;

/**
 * A result of an authentication attempt. May succeed, fail, or be in error.
 * Client code may use normal constructors, or use one of the static convenience 
 * methods ({@code createXXX}).
 * 
 * @author michael
 */
public class UserUsach {

    private String user;
    private String password;
    private String tipo;
    private String rut;

    private String errorCode;
    private String message;

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getRut() {
        return rut;
    }

    public void setRut(String rut) {
        this.rut = rut;
    }
}
