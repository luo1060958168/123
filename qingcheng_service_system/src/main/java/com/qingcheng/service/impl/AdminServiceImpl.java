package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.AdminMapper;
import com.qingcheng.dao.AdminRoleMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.system.Admin;
import com.qingcheng.pojo.system.AdminRole;
import com.qingcheng.pojo.system.GroupAdminRole;
import com.qingcheng.service.system.AdminService;
import com.qingcheng.util.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service(interfaceClass = AdminService.class)
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private AdminRoleMapper adminRoleMapper;

    /**
     * 返回全部记录
     * @return
     */
    public List<Admin> findAll() {
        return adminMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Admin> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Admin> admins = (Page<Admin>) adminMapper.selectAll();
        return new PageResult<Admin>(admins.getTotal(),admins.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Admin> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return adminMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Admin> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Admin> admins = (Page<Admin>) adminMapper.selectByExample(example);
        return new PageResult<Admin>(admins.getTotal(),admins.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public GroupAdminRole findById(Integer id) {
        GroupAdminRole groupAdminRole = new GroupAdminRole();
        Admin admin = adminMapper.selectByPrimaryKey(id);
        //设置密码为空
        admin.setPassword("");
        groupAdminRole.setAdmin(admin);
        // 根据adminId查询角色
        Map map = new HashMap();
        map.put("adminId",id);
        Example example = createExample(map);
        List<AdminRole> adminRoles = adminRoleMapper.selectByExample(example);
        // 得到角色id
        List<Integer> roleIds = new ArrayList<Integer>();
        for (AdminRole adminRole : adminRoles) {
            Integer roleId = adminRole.getRoleId();
            roleIds.add(roleId);
        }
        groupAdminRole.setRoleList(roleIds);
        return groupAdminRole;
    }

    /**
     * 新增
     * @param groupAdminRole
     */
    @Transactional
    public void add(GroupAdminRole groupAdminRole) {
        Admin admin = groupAdminRole.getAdmin();

        // 判断用户名是否存在
        Map map = new HashMap();
        map.put("loginName",admin.getLoginName());
        Example example = createExample(map);
        List<Admin> admins = adminMapper.selectByExample(example);
        if (admins.size()!=0){
            throw new RuntimeException("您注册的用户名已存在");
        }

        String hashpw = BCrypt.hashpw(admin.getPassword(), BCrypt.gensalt());// 给密码加密
        admin.setPassword(hashpw);//设置加密
        adminMapper.insert(admin);
        // 添加用户角色
        Integer adminId = admin.getId();
        List<Integer> roleList = groupAdminRole.getRoleList();
        if (roleList != null){
            for (Integer roleId : roleList) {
                AdminRole adminRole = new AdminRole();
                adminRole.setAdminId(adminId);
                adminRole.setRoleId(roleId);
                adminRoleMapper.insert(adminRole);
            }
        }
    }

    /**
     * 修改
     * @param groupAdminRole
     */
    @Transactional
    public void update(GroupAdminRole groupAdminRole) {
        Admin admin = groupAdminRole.getAdmin();
        String password = admin.getPassword();
        if (password != null && !"".equals(password)){
            // 修改了密码
            admin.setPassword(BCrypt.hashpw(password,BCrypt.gensalt()));
        }else {
            // 没修改密码
            String password1 = adminMapper.selectByPrimaryKey(admin.getId()).getPassword();
            admin.setPassword(password1);
        }
        // 修改用户
        adminMapper.updateByPrimaryKeySelective(admin);

        // 删除角色
        Example example = new Example(AdminRole.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("adminId",admin.getId());
        adminRoleMapper.deleteByExample(example);

        // 添加用户角色
        List<Integer> roleList = groupAdminRole.getRoleList();
        if (roleList.size() != 0){
            for (Integer roleId : roleList) {
                AdminRole adminRole = new AdminRole();
                adminRole.setRoleId(roleId);
                adminRole.setAdminId(admin.getId());
                adminRoleMapper.insert(adminRole);
            }
        }
        return ;
    }

    /**
     *  删除
     * @param id
     */
    public void delete(Integer id) {
        adminMapper.deleteByPrimaryKey(id);
    }


    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Admin.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 用户名
            if(searchMap.get("loginName")!=null && !"".equals(searchMap.get("loginName"))){
                criteria.andLike("loginName", (String) searchMap.get("loginName"));
            }
            // 密码
            if(searchMap.get("password")!=null && !"".equals(searchMap.get("password"))){
                criteria.andLike("password", (String) searchMap.get("password"));
            }
            // 状态
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andLike("status","%"+searchMap.get("status")+"%");
            }

            // id
            if(searchMap.get("id")!=null ){
                criteria.andEqualTo("id",searchMap.get("id"));
            }

        }
        return example;
    }



}
