package com.qingcheng.consumer;

import com.alibaba.fastjson.JSON;
import com.aliyuncs.CommonResponse;
import com.qingcheng.util.SmsUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.swing.text.html.FormSubmitEvent;
import java.rmi.ServerException;
import java.util.Map;

public class SmsMessageConsumer implements MessageListener {

    @Autowired
    private SmsUtil smsUtil;

    @Value("${smsCode}")
    private String smsCode;// 短信模板编号

    @Value("${param}")
    private String param;// 短信参数

    public void onMessage(Message message) {
        String jsonString = new String(message.getBody());
        Map<String,String> map = JSON.parseObject(jsonString, Map.class);
        String phone = map.get("phone");
        String code = map.get("code");
        System.out.println("手机号："+phone + "  验证码："+code);
        String newParam = param.replace("[value]", code);

        // 调用发送短信
        try {
            CommonResponse commonResponse = smsUtil.sendSms(phone, smsCode, newParam);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
