package com.qingcheng.service;

import com.alibaba.fastjson.JSON;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.pojo.seckill.SeckillStatus;
import com.qingcheng.util.IdWorker;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MultiThreadingCreateOrder {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private RabbitTemplate rabbitTemplate;



    /**
     * 多线程下单
     */
    @Async
    public void createdOrder(){
        try {
            Thread.sleep(3000);
            // 获取队列中排队第一个
            SeckillStatus seckillOrderQueue = (SeckillStatus) redisTemplate.boundListOps("SeckillOrderQueue").rightPop();

            if (seckillOrderQueue!=null){
                // 从商品队列中获取一个商品
                Object goodsId = redisTemplate.boundListOps("SeckillGoodsCountList_" + seckillOrderQueue.getGoodsId()).rightPop();
                if (goodsId == null){
                    // 清理当前用户的排队信息
                    clearQueue(seckillOrderQueue);
                    return;
                }
                // 用户抢购商品
                Long id = seckillOrderQueue.getGoodsId();
                // 商品秒杀时间区间
                String time = seckillOrderQueue.getTime();
                // 抢单用户
                String username = seckillOrderQueue.getUsername();
                // 获取商品数据
                SeckillGoods goods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_" + time).get(id);
                // 如果没有库存抛出异常
                if (goods==null || goods.getStockCount() <= 0){
                    throw new RuntimeException("已售罄！");
                }

                // 有库存开始创建订单
                SeckillOrder seckillOrder = new SeckillOrder();
                seckillOrder.setId(idWorker.nextId());
                seckillOrder.setSeckillId(id);
                seckillOrder.setMoney(goods.getCostPrice());
                seckillOrder.setUserId(username);
                seckillOrder.setSellerId(goods.getSellerId());
                seckillOrder.setCreateTime(new Date());
                seckillOrder.setStatus("0");

                // 将秒杀商品存入redis缓存中
                redisTemplate.boundHashOps("SeckillOrder").put(username,seckillOrder);

                //抢单成功更新状态等待支付
                SeckillStatus seckillStatus = (SeckillStatus) redisTemplate.boundHashOps("UserQueueStatus").get(username);
                seckillStatus.setStatus(2);
                seckillStatus.setMoney(seckillOrder.getMoney().floatValue());
                seckillStatus.setOrderId(seckillOrder.getId());
                redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);

                Thread.sleep(3000);

                // 减少库存
                Long surplusCount = redisTemplate.boundHashOps("SeckillGoodsCount").increment(id, -1);// 商品数量递减
                if (surplusCount < 0){
                    throw new RuntimeException("已售罄！");
                }
                goods.setStockCount(surplusCount.intValue());// 根据计数器统计

                // 判断当前商品是否还有库存
                if (goods.getStockCount() <= 0){
                    // 将商品数据同步到mysql中
                    seckillGoodsMapper.updateByPrimaryKeySelective(goods);
                    // 如果没有库存，清空redis中的缓存商品
                    redisTemplate.boundHashOps("SeckillGoods_"+time).delete(id);
                }else {
                    // 有库存从新设置商品到redis中
                    redisTemplate.boundHashOps("SeckillGoods_"+time).put(id,goods);
                }
                // 发送MQ消息
                sendDelayMessage(seckillStatus);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    /***
     * 清理用户排队信息
     * @param seckillOrderQueue
     */
    private void clearQueue(SeckillStatus seckillOrderQueue) {
        // 清理排队标识
        redisTemplate.boundHashOps("UserQueueCount").delete(seckillOrderQueue.getUsername());
        // 清理订单状态标识
        redisTemplate.boundHashOps("UserQueueStatus").delete(seckillOrderQueue.getUsername());

    }

    /***
     * 发送延时消息
     * @param seckillStatus
     */
    public void sendDelayMessage(SeckillStatus seckillStatus){
        rabbitTemplate.convertAndSend("exchange.delay.order.begin", "delay", JSON.toJSONString(seckillStatus), new MessagePostProcessor() {
            public Message postProcessMessage(Message message) throws AmqpException {
                // 消息有效期为30分钟

                message.getMessageProperties().setExpiration(String.valueOf(150000));//测试10秒钟
//                message.getMessageProperties().setExpiration(String.valueOf(1800000));
                return message;
            }
        });

    }
}
