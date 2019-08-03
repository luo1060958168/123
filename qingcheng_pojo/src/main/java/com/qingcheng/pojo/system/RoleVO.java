package com.qingcheng.pojo.system;

import java.io.Serializable;
import java.util.List;

public class RoleVO implements Serializable {
    private Integer id;// 角色id
    private String name;// 角色名称
    private List<Resource> resourceList;// 权限集合

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Resource> getResourceList() {
        return resourceList;
    }

    public void setResourceList(List<Resource> resourceList) {
        this.resourceList = resourceList;
    }
}
