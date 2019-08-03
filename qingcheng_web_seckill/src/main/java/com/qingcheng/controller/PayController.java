package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.github.wxpay.sdk.WXPayUtil;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.service.pay.WeixinPayService;
import com.qingcheng.service.seckill.SeckillOrderService;
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
@RequestMapping(value = "/pay")
public class PayController {

    @Reference
    private SeckillOrderService seckillOrderService;
    @Reference
    private WeixinPayService weixinPayService;

    @GetMapping("/createNative")
    public Map createNative(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // 根据用户名查询秒杀订单
        SeckillOrder seckillOrder = seckillOrderService.findById(username);
        if (seckillOrder != null){
            // 判断当前订单是该用户自己的订单
            if (username.equals(seckillOrder.getUserId())){
                int money = (int) ((seckillOrder.getMoney().doubleValue())*100);
                // 调用微信支付创建二维码
                return weixinPayService.createNative(seckillOrder.getId().toString(),money,"http://tianshi.easy.echosite.cn/pay/notify.do",username);
            }
        }
        return null;
    }

    /**
     * 查询订单状态
     * @param orderId
     * @return
     */
    @GetMapping("/queryPayStatus")
    public Map<String ,String > queryPayStatus(String orderId){
        Map<String, String> resultMap = weixinPayService.queryPayStasus(orderId);
        if (resultMap.get("return_code").equalsIgnoreCase("success")&&resultMap.get("result_code").equalsIgnoreCase("success")){
            // 获取支付状态
            String result = resultMap.get("trade_state");
            if (result.equalsIgnoreCase("success")){
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                seckillOrderService.updatePayStatus(resultMap.get("out_trade_no"),resultMap.get("transaction_id"),username);
            }
        }
        return resultMap;
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
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.close();
            inputStream.close();
            String result = new String(outputStream.toByteArray(), "utf-8");
            System.out.println(result);

            // 处理回调信息
            Map<String, String> map = WXPayUtil.xmlToMap(result);
            seckillOrderService.updatePayStatus(map.get("out_trade_no"),map.get("attach"),map.get("transaction_id"));


        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashMap();
    }
}
