package com.qingcheng.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.pojo.seckill.SeckillStatus;
import com.qingcheng.service.pay.WeixinPayService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderMessageListener implements MessageListener {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Reference
    private WeixinPayService weixinPayService;
        /**
         * 监听消息
         */
    public void onMessage(Message message) {

        try {
            String content = new String(message.getBody());
            // 将消息转换为seckillstatus
            SeckillStatus seckillStatus = JSON.parseObject(content, SeckillStatus.class);

            // 订单回滚处理
            rollbackOrder(seckillStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 订单回滚处理
     * @param seckillStatus
     */
    public void rollbackOrder(SeckillStatus seckillStatus) throws Exception {
        // 获取redis中订单信息
        String username = seckillStatus.getUsername();
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(username);

        // 如果redis中有订单信息说明未支付
        if (seckillOrder!=null){
            // 关闭支付
            Map<String, String> closeResult = weixinPayService.closePay(seckillStatus.getOrderId());
            if (closeResult.get("return_code").equalsIgnoreCase("success") && closeResult.get("result_code").equalsIgnoreCase("success")){
                // 删除订单
                redisTemplate.boundHashOps("SeckillOrder").delete(username);
                // 库存回滚
                // 1. 获取该商品
                SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_" + seckillStatus.getTime()).get(seckillStatus.getGoodsId());

                // 2.如果redis中没有，则从数据库中加载
                if (seckillGoods == null){
                    seckillGoods = seckillGoodsMapper.selectByPrimaryKey(seckillStatus.getGoodsId());
                }

                //3.数量，递增， 队列 +1
                Long surplusCount = redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillStatus.getGoodsId(), 1);
                seckillGoods.setStockCount(surplusCount.intValue());
                seckillGoodsMapper.updateByPrimaryKeySelective(seckillGoods);
                redisTemplate.boundListOps("SeckillGoodsCountList_"+seckillStatus.getGoodsId()).leftPush(seckillStatus.getGoodsId());

                // 4.数据同步到redis中
                redisTemplate.boundHashOps("SeckillGoods_"+seckillStatus.getTime()).put(seckillStatus.getGoodsId(),seckillGoods);

                // 清理排队标识
                redisTemplate.boundHashOps("UserQueueCount").delete(seckillStatus.getUsername());
                // 清理抢单标识
                redisTemplate.boundHashOps("UserQueueStatus").delete(seckillStatus.getUsername());

            }
        }

    }
}
