package edu.harvard.iq.dataverse.authorization;

import java.util.List;

/**
 * A result of an authentication attempt. May succeed, fail, or be in error.
 * Client code may use normal constructors, or use one of the static convenience 
 * methods ({@code createXXX}).
 * 
 * @author michael
 */
public class AssigmentsUsachResponse {

    private String status;
    private List<AssigmentRecord> data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<AssigmentRecord> getData() {
        return data;
    }

    public void setData(List<AssigmentRecord> data) {
        this.data = data;
    }
}
