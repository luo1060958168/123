package com.qingcheng.service.order;

import java.util.List;
import java.util.Map;

/**
 * 购物车服务
 */
public interface CartService {
    /**
     * 根据用户名从redis中提取购物车数据
     * @param username
     * @return
     */
    public List<Map<String, Object>> findCartList(String username);

    /**
     * 添加商品到购物车
     * @param username
     * @param skuId
     * @param num
     */
    public void addItem(String username, String skuId, Integer num);

    /**
     * 更新选中状态
     * @param username
     * @param id
     * @param checked
     * @return
     */
    public boolean updateChecked(String username, String id, boolean checked);

    /**
     * 删除选中
     * @param username
     */
    public void deleteCheckedCart(String username);

    /**
     * 计算优惠金额
     * @param username
     * @return
     */
    public int preferential(String username);

    /**
     * 刷新购物车商品及价格
     * @param username
     * @return
     */
    public List<Map<String, Object>> findNewOrderItemList(String username);
}
