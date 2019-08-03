package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.seckill.SeckillStatus;
import com.qingcheng.service.seckill.SeckillOrderService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/seckill/order")
public class SeckillOrderController {

    @Reference
    private SeckillOrderService seckillOrderService;


    @RequestMapping("/add")
    public Result add(String time, Long id){
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if ("anonymousUser".equals(username)){
                // 用户没有 登录
                return new Result(403,"请先登录!");
            }
            
            // 调用service增加订单
            Boolean bo = seckillOrderService.add(id, time, username);
            if (bo){
                //下单成功
                return new Result(0,"下单成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(2,e.getMessage());
        }

        return new Result(1,"秒杀失败");
    }

    @RequestMapping(value = "/query")
    public Result queryStatus(){
        // 获取用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 如果是匿名账号
        if ("anonymousUser".equals(username)){
            // 403错误表示未登录
            return new Result(403,"请先登录");
        }

        // 根据用户名查询 抢单状态
        SeckillStatus seckillStatus = seckillOrderService.queryStatus(username);
        if (seckillStatus!=null){
            Result result = new Result(seckillStatus.getStatus(), "抢购状态");
            seckillStatus.setOrderIdStr(seckillStatus.getOrderId().toString());
            result.setOther(seckillStatus);
            return result;
        }
        return new Result(404,"没有抢购信息");
    }



}
