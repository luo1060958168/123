package com.qingcheng.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.service.seckill.SeckillGoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    /**
     * 获取指定时间对应的秒杀商品列表
     * @param key
     * @return
     */
    @Autowired
    private RedisTemplate redisTemplate;
    public List<SeckillGoods> list(String key) {
        return redisTemplate.boundHashOps("SeckillGoods_" + key).values();
    }

    /***
     * 根据id查询商品详情
     * @param time：时间区间
     * @param id:商品id
     * @return
     */
    public SeckillGoods one(String time, Long id) {
        return (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_"+time).get(id);
    }
}
