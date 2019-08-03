package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.service.seckill.SeckillGoodsService;
import com.qingcheng.util.DateUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(value = "/seckill/goods")
public class SeckillGoodsController {

    @Reference
    private SeckillGoodsService seckillGoodsService;

    /**
     * 查询秒杀时间段菜单
     * @return
     */
    @RequestMapping("/menus")
    public List<Date> dateMenus(){
        return DateUtil.getDateMenus();
    }

    @RequestMapping("/list")
    public List<SeckillGoods> list(String time){
        String timeFormat = DateUtil.formatStr(time);
        // 调用service 查询数据
        return seckillGoodsService.list(timeFormat);
    }

    /**
     * 根据id查询商品详情
     * @param time
     * @param id
     * @return
     */
    @RequestMapping("/one")
    public SeckillGoods one(String time, Long id){
        // 调用service查询商品详情
        return seckillGoodsService.one(time, id);
    }


}
