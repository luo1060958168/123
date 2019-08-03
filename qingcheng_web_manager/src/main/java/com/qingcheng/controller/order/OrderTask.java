package com.qingcheng.controller.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.order.CategoryReportService;
import com.qingcheng.service.order.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

//@RestController
//@RequestMapping("/create")
@Component
public class OrderTask {

    @Reference
    private CategoryReportService categoryReportService;

    @Reference
    private OrderService orderService;

//    @Scheduled(cron = "* * * * * ?")
//    public void test(){
//        System.out.println(new Date());
//    }

//    @GetMapping("/data")
    @Scheduled(cron = "0 0 1 * * ?")
    public void createCategoryReportData(){
        System.out.println("createCategoryReportData");
        categoryReportService.createData();
    }

//    @Scheduled(cron = "0 0/2 * * * ?")
//    public void orderTimeOutLogic(){
//        System.out.println("每两分钟执行一次超时关闭"+new Date());
//        orderService.orderTimeOutLogic();
//    }
}
