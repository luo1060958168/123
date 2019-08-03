package com.qingcheng.service.pay;

import java.util.Map;

public interface WeixinPayService {

    /**
     * 生成支付二维码
     * @param orderId
     * @param money
     * @param notifyUrl
     * @return
     */
    public Map createNative(String orderId, Integer money, String notifyUrl, String... attach);

    /**
     * 微信支付回调
     * @param xml
     */
    public void notifyLogic(String xml);

    /**
     * 查询订单支付状态
     * @param orderId
     * @return
     */
    public Map queryPayStasus(String orderId);

    /**
     * 关闭支付订单
     * @param orderId
     * @return
     * @throws Exception
     */
    public Map<String, String> closePay(Long orderId) throws Exception;

}
