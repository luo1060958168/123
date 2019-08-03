package com.qingcheng.service.goods;

import com.qingcheng.pojo.order.OrderItem;

import java.util.List;

public interface StockBackService {
    public void addList(List<OrderItem> orderItems);

    public void doBack();
}
