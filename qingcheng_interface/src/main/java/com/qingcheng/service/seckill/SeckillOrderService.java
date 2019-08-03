package com.qingcheng.service.seckill;

import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.pojo.seckill.SeckillStatus;

public interface SeckillOrderService {

    /**
     * 添加秒杀订单
     * @param id：商品id
     * @param time：商品秒杀开始时间
     * @param username：用户登录名
     * @return
     */
    Boolean add(Long id, String time, String username) throws Exception;


    /***
     * 根据用户名查询抢单状态
     * @param username
     * @return
     */
    public SeckillStatus queryStatus(String username);

    /***
     * 更新订单状态
     * @param out_trade_no
     * @param transaction_id
     * @param username
     */
    void updatePayStatus(String out_trade_no, String transaction_id, String username);

    /***
     * 根据用户名查询redis缓存中的订单
     * @param username
     * @return
     */
    SeckillOrder findById(String username);
}
