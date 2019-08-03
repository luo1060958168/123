package com.qingcheng.controller.goods;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.StockBackService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SkuTask {

    @Reference
    private StockBackService stockBackService;

    @Scheduled(cron = "0/30 * *  * * ?")
    public void orderTimeOutLogic(){
        System.out.println("执行订单回滚");
        stockBackService.doBack();
    }
}
