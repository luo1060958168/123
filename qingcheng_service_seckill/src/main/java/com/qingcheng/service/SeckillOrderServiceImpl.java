package com.qingcheng.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.dao.SeckillOrderMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.pojo.seckill.SeckillStatus;
import com.qingcheng.service.seckill.SeckillOrderService;
import com.qingcheng.util.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;

@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private MultiThreadingCreateOrder multiThreadingCreateOrder;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    /**
     * 添加订单
     * @param id：商品id
     * @param time：商品秒杀开始时间
     * @param username：用户登录名
     * @return
     */
    public Boolean add(Long id, String time, String username) throws Exception {
        Long size = redisTemplate.boundListOps("SeckillGoodsCountList_" + id).size();
        if (size == null || size <= 0){
            // 该商品没有库存
            throw new RuntimeException("101");
        }
        // 重复排队
        Long userQueueCount = redisTemplate.boundHashOps("UserQueueCount").increment(username, 1);
        if (userQueueCount>1){
            // 表示有重复排队
            throw new RuntimeException("100");
        }
        // 封装排队信息
        SeckillStatus seckillStatus = new SeckillStatus(username, new Date(), 1, id, time);
        // 将秒杀排队信息存入队列
        redisTemplate.boundListOps("SeckillOrderQueue").leftPush(seckillStatus);
        // 将抢单状态存入redis中
        redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);
        // 多线程下单
        multiThreadingCreateOrder.createdOrder();

        return true;
    }


    /**
     * 查询抢单状态
     * @param username
     * @return
     */
    public SeckillStatus queryStatus(String username){
        SeckillStatus seckillStatus = (SeckillStatus) redisTemplate.boundHashOps("UserQueueStatus").get(username);
        return seckillStatus;
    }

    /***
     * 更新订单状态
     * @param out_trade_no
     * @param transaction_id
     * @param username
     */
    public void updatePayStatus(String out_trade_no, String transaction_id, String username) {
        // 从redis中中查询订单信息
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(username);
        // 设置订单状态
        seckillOrder.setStatus("1");
        // 支付时间
        seckillOrder.setPayTime(new Date());
        // 同步到mysql中
        seckillOrderMapper.insertSelective(seckillOrder);

        // 清空redis缓存
        redisTemplate.boundHashOps("SeckillOrder").delete(username);
        // 清除排队信息
        redisTemplate.boundHashOps("SeckillQueueCount").delete(username);
        // 清除抢购状态
        redisTemplate.boundHashOps("SeckillQueueStatus").delete(username);

    }

    /***
     * 根据用户名查询缓存中的秒杀订单
     * @param username
     * @return
     */
    public SeckillOrder findById(String username){
        // 查询订单
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(username);
        return seckillOrder;
    }
}
