package edu.harvard.iq.dataverse.authorization;

/**
 * A result of an authentication attempt. May succeed, fail, or be in error.
 * Client code may use normal constructors, or use one of the static convenience 
 * methods ({@code createXXX}).
 * 
 * @author michael
 */
public class AssigmentRecord {

    private Long id;
    private String assignee;
    private String roleId;
    private String _roleAlias;
    private String definitionPointId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String get_roleAlias() {
        return _roleAlias;
    }

    public void set_roleAlias(String _roleAlias) {
        this._roleAlias = _roleAlias;
    }

    public String getDefinitionPointId() {
        return definitionPointId;
    }

    public void setDefinitionPointId(String definitionPointId) {
        this.definitionPointId = definitionPointId;
    }
}
