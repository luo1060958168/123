package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.SkuMapper;
import com.qingcheng.dao.StockBackMapper;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.pojo.order.StockBack;
import com.qingcheng.service.goods.StockBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service(interfaceClass = StockBackService.class)
public class StockBackServiceImpl implements StockBackService {

    @Autowired
    private StockBackMapper stockBackMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Transactional
    public void addList(List<OrderItem> orderItems) {
        for (OrderItem orderItem : orderItems) {
            StockBack stockBack = new StockBack();
            stockBack.setOrderId(orderItem.getOrderId());
            stockBack.setSkuId(orderItem.getSkuId());
            stockBack.setStatus("0");
            stockBack.setNum(orderItem.getNum());
            stockBack.setCreateTime(new Date());
            stockBackMapper.insert(stockBack);
        }
    }

    /**
     * 执行商品回滚
     */
    @Transactional
    public void doBack(){
        System.out.println("开始库存回滚任务");
        StockBack stockBack0 = new StockBack();
        stockBack0.setStatus("0");
        List<StockBack> stockList = stockBackMapper.select(stockBack0);
        for (StockBack stockBack : stockList) {
            //添加库存
            skuMapper.deductionStock(stockBack.getSkuId(), -stockBack.getNum() );
            //减少销量
            skuMapper.addSaleNum(stockBack.getSkuId(), -stockBack.getNum());
            stockBack.setStatus("1");
            stockBack.setBackTime(new Date());
            stockBackMapper.updateByPrimaryKey(stockBack);
        }
        System.out.println("库存回滚任务结束");
    }


}
