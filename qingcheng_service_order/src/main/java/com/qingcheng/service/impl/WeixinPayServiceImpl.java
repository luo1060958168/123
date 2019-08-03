package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.wxpay.sdk.Config;
import com.github.wxpay.sdk.WXPayRequest;
import com.github.wxpay.sdk.WXPayUtil;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.pay.WeixinPayService;
import com.qingcheng.util.HttpClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

@Service
public class WeixinPayServiceImpl implements WeixinPayService {

    @Autowired
    private Config config;
    @Reference
    private OrderService orderService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Map createNative(String orderId, Integer money, String notifyUrl,String... attach) {
        try {
            //1.创建参数
            Map<String,String> param=new HashMap();//创建参数
            param.put("appid", config.getAppID());//公众号
            param.put("mch_id", config.getMchID());//商户号
            param.put("nonce_str", WXPayUtil.generateNonceStr());//随机字符串
            param.put("body", "青橙");//商品描述
            param.put("out_trade_no", orderId);//商户订单号
            param.put("total_fee",money+"");//总金额（分）
            param.put("spbill_create_ip", "127.0.0.1");//IP
            param.put("notify_url", notifyUrl );//暂时随便写一个
            param.put("trade_type", "NATIVE");//交易类型
            if (attach != null && attach.length>0){
                param.put("attach",attach[0]);
            }
            String xmlParam = WXPayUtil.generateSignedXml(param,config.getKey());
            //2.发送请求
            WXPayRequest wxPayRequest = new WXPayRequest(config);
            String result = wxPayRequest.requestWithCert("/pay/unifiedorder", null, xmlParam, false);
            //3.封装结果
            System.out.println(result);
            Map<String, String> resultMap = WXPayUtil.xmlToMap(result);
            Map<String, String> map=new HashMap();
            map.put("code_url", resultMap.get("code_url"));//支付地址
            map.put("total_fee", money+"");//总金额
            map.put("out_trade_no",orderId);//订单号
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap();
        }
    }

    @Override
    public void notifyLogic(String xml) {
        try {
            //1.对xml进行解析
            Map<String, String> map = WXPayUtil.xmlToMap(xml);
            //2.验证签名
            boolean signatureValid = WXPayUtil.isSignatureValid(map, config.getKey());

            System.out.println("验证签名是否正确："+signatureValid);
            System.out.println(map.get("out_trade_no"));
            System.out.println(map.get("result_code"));
            if (signatureValid) {
                if ("SUCCESS".equals(map.get("result_code"))){
                    rabbitTemplate.convertAndSend("paynotify","",map.get("out_trade_no"));
                    orderService.updatePayStatus(map.get("out_trade_no"),map.get("transaction_id"));//更新订单状态
                }else {
                    System.out.println("支付不成功");
                    // 记录日志
                }

            }else {
                System.out.println("非法签名");
                // 记录日志
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询订单状态
     * @param orderId
     * @return
     */
    @Override
    public Map queryPayStasus(String orderId) {

        try { // 参数设置
            Config config = new Config();
            HashMap<String, String> paramMap = new HashMap<>();
            paramMap.put("appid",config.getAppID());// 应用id
            paramMap.put("mch_id",config.getMchID());// 商户编号
            paramMap.put("nonce_str",WXPayUtil.generateNonceStr());// 随机字符
            paramMap.put("out_trade_no",String.valueOf(orderId));// 商家唯一编号
            // 将map数据转换成xml字符
            String xmlParam = WXPayUtil.generateSignedXml(paramMap, config.getKey());

            // 确定url
            String url = "https://api.mch.weixin.qq.com/pay/orderquery";

            // 发送请求
            HttpClient httpClient = new HttpClient(url);
            // https
            httpClient.setHttps(true);
            // 提交参数
            httpClient.setXmlParam(xmlParam);
            // 提交
            httpClient.post();
            //获取返回数据
            String content = httpClient.getContent();
            // 将返回数据解析成map
            return WXPayUtil.xmlToMap(content);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 关闭微信支付订单
     * @param orderId
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, String> closePay(Long orderId) throws Exception {
        // 参数设置
        Config config = new Config();
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("appid",config.getAppID());// 应用id
        paramMap.put("mch_id",config.getMchID());// 商户编号
        paramMap.put("nonce_str",WXPayUtil.generateNonceStr());// 随机字符
        paramMap.put("out_trade_no",String.valueOf(orderId));// 商家唯一编号

        // 将map数据转换成xml字符
        String xmlParam = WXPayUtil.generateSignedXml(paramMap, config.getKey());

        // 确定url
        String url = "https://api.mch.weixin.qq.com/pay/closeorder";

        // 发送请求
        HttpClient httpClient = new HttpClient(url);
        // https
        httpClient.setHttps(true);
        // 提交参数
        httpClient.setXmlParam(xmlParam);
        // 提交
        httpClient.post();
        //获取返回数据
        String content = httpClient.getContent();
        // 将返回数据解析成map
        return WXPayUtil.xmlToMap(content);
    }
}
