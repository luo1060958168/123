package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.pojo.goods.Category;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.PreferentialService;
import com.qingcheng.util.CacheKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Reference
    private SkuService skuService;
    @Reference
    private CategoryService categoryService;
    @Autowired
    private PreferentialService preferentialService;

    @Override
    public List<Map<String, Object>> findCartList(String username) {
        System.out.println("从redis中提取购物车："+username);
        List<Map<String,Object>> cartList = (List<Map<String, Object>>) redisTemplate.boundHashOps(CacheKey.CART_LIST).get(username);
        if (cartList == null){
            return new ArrayList<>();
        }
        return cartList;
    }

    /**
     * 添加商品到购物车及删除商品
     * @param username
     * @param skuId
     * @param num
     */
    @Override
    public void addItem(String username, String skuId, Integer num) {
        // 遍历购物车，如果商品不存在则添加商品到购物车，如果存在则累加数量

        List<Map<String, Object>> cartList = findCartList(username);

        Boolean flag = false;//默认购物车中不存在

        for (Map<String, Object> map : cartList) {
            OrderItem item = (OrderItem) map.get("item");
            if (item.getSkuId().equals(skuId)){// 购物车中存在该商品
                if (item.getNum() <= 0){//如果小于等于0
                    cartList.remove(map);// 删除购物车中的商品
                    break;
                }
                int weight = item.getWeight()/item.getNum();// 得到单个商品的重量
                item.setNum(item.getNum()+num);//累加数量
                if (item.getNum() <= 0){
                    cartList.remove(map);// 商品数量小于等于0 删除商品
                    flag=true;
                    break;
                }
                item.setWeight(weight * item.getNum());// 总总量
                item.setMoney(item.getPrice() * item.getNum());// 得到总金额
                flag = true;
                break;// 已经找到并修改该商品返回
            }

        }

        //购物车中不存在该商品
        if (flag == false){
            Sku sku = skuService.findById(skuId);

            if (sku == null){
                throw new RuntimeException("商品不存在");
            }

            if (!"1".equals(sku.getStatus())){
                throw new RuntimeException("商品状态不合法");
            }

            if (num <= 0){
                System.out.println("商品数量不合法");
                throw new RuntimeException("商品数量不合法");
            }

            OrderItem orderItem = new OrderItem();

            orderItem.setSkuId(skuId);
            orderItem.setSpuId(sku.getSpuId());
            orderItem.setNum(num);
            orderItem.setImage(sku.getImage());
            orderItem.setPrice(sku.getPrice());
            orderItem.setName(sku.getName());
            orderItem.setMoney(sku.getPrice()*num);// 计算金额
            if (sku.getWeight() == null){
                sku.setWeight(0);
            }
            orderItem.setWeight(sku.getWeight()*num);// 计算重量

            // 商品分类
            orderItem.setCategoryId3(sku.getCategoryId());//设置三级分类id

            // 先到redis中查询category分类
            Category category3 = (Category) redisTemplate.boundHashOps(CacheKey.CATEGORY).get(sku.getCategoryId());
            if (category3 == null){
                // 获取3级分类
                category3 = categoryService.findById(sku.getCategoryId());
                // 存到缓存中
                redisTemplate.boundHashOps(CacheKey.CATEGORY).put(sku.getCategoryId(),category3);
            }
            orderItem.setCategoryId2(category3.getParentId());//设置二级分类id

            // 得到二级分类
            Category category2 = (Category) redisTemplate.boundHashOps(CacheKey.CATEGORY).get(category3.getParentId());
            if (category2 == null){
                category2 = categoryService.findById(category3.getParentId());
            }
            orderItem.setCategoryId1(category2.getParentId());// 设置一级分类id

            // 添加商品进购物车
            Map map = new HashMap();
            map.put("checked",true);// 默认选中
            map.put("item",orderItem);
            cartList.add(map);
        }

        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);

    }

    /**
     * 更改购物车选中状态
     * @param username
     * @param id
     * @param checked
     * @return
     */
    @Override
    public boolean updateChecked(String username, String id, boolean checked) {
        List<Map<String,Object>> cartList = (List<Map<String, Object>>) redisTemplate.boundHashOps(CacheKey.CART_LIST).get(username);
        boolean isOK = false;// 是否存在该商品
        for (Map<String, Object> map : cartList) {
            OrderItem item = (OrderItem) map.get("item");
            if (item.getSkuId().equals(id)){//修改选中状态
                map.put("checked",checked);
                isOK = true;// 修改成功
                break;
            }
        }

        // 更新缓存
        if (isOK){
            redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);
        }
        return isOK;
    }

    /**
     * 删除选中的购物车
     * @param username
     */
    public void deleteCheckedCart(String username) {
        // 获取未选中的购物车
        List<Map<String, Object>> cartList = findCartList(username).stream().filter(cart -> (boolean) cart.get("checked") == false).collect(Collectors.toList());
        // 存入缓存
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);
    }

    /**
     * 计算当前选中购物车的优惠金额
     * @param username
     * @return
     */
    @Override
    public int preferential(String username) {
        // 获取购物车中选中的商品
        List<OrderItem> orderItemList = findCartList(username).stream().filter(cart -> (boolean) cart.get("checked") == true).map(cart -> (OrderItem)cart.get("item")).collect(Collectors.toList());
        //进行分类得到每个分类的金额
        Map<Integer, IntSummaryStatistics> cartMap = orderItemList.stream().collect(Collectors.groupingBy(OrderItem::getCategoryId3, Collectors.summarizingInt(OrderItem::getMoney)));

        int allPreferential = 0;// 总优惠金额
        // 遍历得到各分类金额
        for (Integer categoryId : cartMap.keySet()) {
            // 分类金额
            int money = (int)cartMap.get(categoryId).getSum();
            // 分类优惠金额
            int preMoney = preferentialService.findPreMoneyByCategoryId(categoryId, money);
            System.out.println("分类："+categoryId+"   消费金额："+money + "   优惠："+preMoney);
            allPreferential += preMoney;
        }
        return allPreferential;
    }

    /**
     * 刷新购物车及价格
     * @param username
     * @return
     */
    @Override
    public List<Map<String, Object>> findNewOrderItemList(String username) {
        List<Map<String, Object>> cartList = findCartList(username);
        // 遍历跟新购物车商品价格
        for (Map<String, Object> cart : cartList) {
            OrderItem orderItem = (OrderItem) cart.get("item");
            Integer price = skuService.findPrice(orderItem.getSkuId());
            orderItem.setPrice(price);//跟新价格
            orderItem.setMoney(price*orderItem.getNum());//跟新金额
        }
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);// 存入缓存
        return cartList;
    }
}
