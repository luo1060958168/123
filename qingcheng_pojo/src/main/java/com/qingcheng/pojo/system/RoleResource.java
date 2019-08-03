package com.qingcheng.pojo.system;

import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Table(name = "tb_role_resource")
public class RoleResource implements Serializable{

    @Id
    private Integer roleId;
    @Id
    private Integer ResourceId;

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public Integer getResourceId() {
        return ResourceId;
    }

    public void setResourceId(Integer resourceId) {
        ResourceId = resourceId;
    }
}
