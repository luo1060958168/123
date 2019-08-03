package com.qingcheng.timer;

import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class SeckillGoodsPushTask {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 定时方法，没三十秒执行一次
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void loadGoodsPushRedis(){
        // 获取时间段集合
        List<Date> dateMenus = DateUtil.getDateMenus();
        // 循环时间段
        for (Date startTime : dateMenus) {
            // namespace = SeckillGoods_extName 格式yyyyMMDDHH
            String extName = DateUtil.date2Str(startTime);

            // 根据时间段数据查询对应的秒杀商品
            Example example = new Example(SeckillGoods.class);
            Example.Criteria criteria = example.createCriteria();
            // 1.商品必须通过审核 status=1
            criteria.andEqualTo("status","1");
            // 2.库存>0
            criteria.andGreaterThan("stockCount",0);
            // 3.开始时间<=活动开始时间
            criteria.andGreaterThanOrEqualTo("startTime",startTime);
            // 4.活动结束时间<开始时间+2小时
            criteria.andLessThan("endTime",DateUtil.addDateHour(startTime,2));
            // 5.排除之前已经加载到redis缓存中的商品数据
            Set keys = redisTemplate.boundHashOps("SeckillGoods_" + extName).keys();
            if (keys != null && keys.size() > 0){
                criteria.andNotIn("id",keys);
            }

            // 查询数据
            List<SeckillGoods> seckillGoods = seckillGoodsMapper.selectByExample(example);

            // 将秒杀商品存入到redis缓存中
            for (SeckillGoods seckillGood : seckillGoods) {
                redisTemplate.boundHashOps("SeckillGoods_" + extName).put(seckillGood.getId(),seckillGood);
                Long[] ids = pushIds(seckillGood.getStockCount(), seckillGood.getId());
                // 商品队列存储防止高并发超卖
                redisTemplate.boundListOps("SeckillGoodsCountList_"+seckillGood.getId()).leftPushAll(ids);
                // 自增计数器
                redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillGood.getId(),seckillGood.getStockCount());
            }

        }

    }

    /***
     * 将商品id数据存入库数组中
     * @param len:长度
     * @param id：值
     * @return
     */
    public Long[] pushIds(int len, Long id){
        Long[] ids = new Long[len];
        for (int i = 0; i <ids.length; i++){
            ids[i]=id;
        }
        return ids;
    }
}
