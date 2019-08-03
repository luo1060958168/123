package com.qingcheng.controller.system;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.PageResult;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.system.Admin;
import com.qingcheng.pojo.system.GroupAdminRole;
import com.qingcheng.service.system.AdminService;
import org.springframework.web.bind.annotation.*;

import java.security.acl.Group;
import java.util.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Reference
    private AdminService adminService;

    @GetMapping("/findAll")
    public List<Admin> findAll(){
        return adminService.findAll();
    }

    @GetMapping("/findPage")
    public PageResult<Admin> findPage(int page, int size){
        return adminService.findPage(page, size);
    }

    @PostMapping("/findList")
    public List<Admin> findList(@RequestBody Map<String,Object> searchMap){
        return adminService.findList(searchMap);
    }

    @PostMapping("/findPage")
    public PageResult<Admin> findPage(@RequestBody Map<String,Object> searchMap,int page, int size){
        return  adminService.findPage(searchMap,page,size);
    }

    @GetMapping("/findById")
    public GroupAdminRole findById(Integer id){
        return adminService.findById(id);
    }


    @PostMapping("/add")
    public Result add(@RequestBody GroupAdminRole groupAdminRole){
        adminService.add(groupAdminRole);
        return new Result();
    }

    @PostMapping("/update")
    public Result update(@RequestBody GroupAdminRole groupAdminRole){
        adminService.update(groupAdminRole);
        return new Result();
    }

    @GetMapping("/delete")
    public Result delete(Integer id){
        adminService.delete(id);
        return new Result();
    }

}
