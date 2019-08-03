package com.qingcheng.dao;

import com.qingcheng.pojo.system.Resource;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface ResourceMapper extends Mapper<Resource> {

    @Select("select res_key from tb_resource where id in (" +
            "select resource_id from tb_role_resource where role_id in(" +
            "select role_id from tb_admin_role where admin_id in(" +
            "select id from tb_admin where login_name = ${logName})))")
    public List<String> findResKeyByLoginName(String logName);
}
