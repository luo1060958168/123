package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.pay.WeixinPayService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/wxpay")
public class WxPayController {

    @Reference
    private OrderService orderService;

    @Reference
    private WeixinPayService weixinPayService;

    @GetMapping("/createNative")
    public Map createNative(String orderId){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Order order = orderService.findById(orderId);
        if (order != null){
            if ("0".equals(order.getPayStatus()) && "0".equals(order.getOrderStatus())){

                return weixinPayService.createNative(orderId, order.getPayMoney(), "http://tianshi.easy.echosite.cn/wxpay/notify.do");
            }
        }
        return null;
    }

    @RequestMapping("/notify")
    public Map notifyLogic(HttpServletRequest request){
        System.out.println("支付成功回调。。。");
        InputStream inputStream;

        try {
            inputStream = request.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while((len = inputStream.read(buffer)) != -1){
                outputStream.write(buffer,0,len);
            }
            outputStream.close();
            inputStream.close();
            String result = new String(outputStream.toByteArray(),"utf-8");
            System.out.println(result);

            // 处理回调信息
            weixinPayService.notifyLogic(result);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashMap();
    }
}
