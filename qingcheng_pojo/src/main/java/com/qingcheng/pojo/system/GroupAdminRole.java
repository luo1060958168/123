package com.qingcheng.pojo.system;

import java.io.Serializable;
import java.util.List;

public class GroupAdminRole implements Serializable{
    private Admin admin;
    private List<Integer> roleList;

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public List<Integer> getRoleList() {
        return roleList;
    }

    public void setRoleList(List<Integer> roleList) {
        this.roleList = roleList;
    }
}
