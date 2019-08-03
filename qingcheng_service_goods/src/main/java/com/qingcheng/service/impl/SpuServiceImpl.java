package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.*;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.*;
import com.qingcheng.service.goods.SpuService;
import com.qingcheng.util.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Service(interfaceClass = SpuService.class)
@Transactional
public class SpuServiceImpl implements SpuService {

    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private CategoryBrandMapper categoryBrandMapper;
    @Autowired
    private SpuLogMapper spuLogMapper;

    /**
     * 商品审核
     * @param id
     * @param status
     * @param message
     */
    public void audit(String id, String status, String message) {
        Spu spu = new Spu();
        spu.setId(id);
        if ("1".equals(status)){// 审核通过
            spu.setIsMarketable("1");// 商品上架
        }
        spu.setStatus(status);
        spuMapper.updateByPrimaryKeySelective(spu);


        // 记录商品审核日志
        SpuLog spuLog = new SpuLog();
        spuLog.setSpuId(spu.getId());//商品id
        spuLog.setStatus(spu.getStatus());// 审核状态


        // 记录商品日志
        spuLog.setIsMarketable(spu.getIsMarketable());// 是否上架
        spuLog.setOperation("{操作人:admin,"+"时间:"+new Date().toString()+"}");
        spuLogMapper.insert(spuLog);
    }

    /**
     * 商品下架
     * @param id
     */
    public void pull(String id) {
        Spu spu = new Spu();
        spu.setId(id);
        spu.setIsMarketable("0");// 下架状态
        spuMapper.updateByPrimaryKeySelective(spu);

        // 商品日志
        SpuLog spuLog = new SpuLog();
        spuLog.setIsMarketable("0");
        spuLog.setOperation("{操作人:admin,"+"时间:"+new Date().toString()+"}");
        spuLogMapper.insert(spuLog);
    }

    /**
     * 商品上架
     * @param id
     */
    public void put(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (!"1".equals(spu.getStatus())){
            throw new RuntimeException("商品未审核通过");
        }
        spu.setIsMarketable("1");
        spuMapper.updateByPrimaryKeySelective(spu);

        // 记录商品上架日志
        SpuLog spuLog = new SpuLog();
        spuLog.setIsMarketable("1");
        spuLog.setOperation("{操作人:admin,"+"时间:"+new Date().toString()+"}");
        spuLogMapper.insert(spuLog);
    }

    /**
     * 批量上架
     * @param ids
     */
    public int putMany(Long[] ids) {
        Spu spu = new Spu();
        spu.setIsMarketable("1");// 设置上架
        // 批量修改
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", Arrays.asList(ids));
        criteria.andEqualTo("isMarketable","0");// 下架状态
        criteria.andEqualTo("status","1");// 审核通过
        criteria.andEqualTo("isDelete","0");// 未删除的
        return spuMapper.updateByExampleSelective(spu,example);
    }

    /**
     * 批量下架
     * @param ids
     * @return
     */
    public int pullMany(Long[] ids) {
        Spu spu = new Spu();
        spu.setIsMarketable("0");// 设置下架
        //批量下架
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id",Arrays.asList(ids));
        criteria.andEqualTo("isMarketable","1");// 上架状态
        criteria.andEqualTo("status","1");// 审核通过
        criteria.andEqualTo("isDelete","0"); //未删除的
        return spuMapper.updateByExampleSelective(spu,example);
    }

    /**
     * 根据id查询goods
     * @param id
     * @return
     */
    public Goods findGoodsById(String id) {
        Goods goods = new Goods();
        //查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //查询sku集合
        Example example = new Example(Sku.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("spuId",id);
        List<Sku> skuList = skuMapper.selectByExample(example);
        //封装返回
        goods.setSpu(spu);
        goods.setSkuList(skuList);
        return goods;
    }

    /**
     * 保存商品
     * @param goods
     */
    public void saveGoods(Goods goods) {
        // 保存spu信息
        Spu spu = goods.getSpu();
        if (spu.getId() == null){//新增商品
            spu.setId(idWorker.nextId()+"");
            spuMapper.insert(spu);
        }else {// 修改
            Example example = new Example(Sku.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("spuId",spu.getId());
            // 删除sku集合
            skuMapper.deleteByExample(example);
            // 修改spu
            spuMapper.updateByPrimaryKeySelective(spu);
        }

        // 保存category brand;关联品牌和分类
        CategoryBrand categoryBrand = new CategoryBrand();
        categoryBrand.setBrandId(spu.getBrandId());
        categoryBrand.setCategoryId(spu.getCategory3Id());
        int count = categoryBrandMapper.selectCount(categoryBrand);
        if (count==0){
            categoryBrandMapper.insert(categoryBrand);
        }

        //保存sku列表信息

        Date date = new Date();// 创建时间
        //分类对象
        Category category = categoryMapper.selectByPrimaryKey(spu.getCategory3Id());
        String categoryName = category.getName();// 分类名称

        List<Sku> skuList = goods.getSkuList();
        for (Sku sku : skuList) {

            if (sku.getId()==null){// 新增sku
                sku.setId(idWorker.nextId()+"");
                sku.setCreateTime(date);// 创建日期
            }
            if (sku.getSpec() == null || "".equals(sku.getSpec())){// 无规格
                sku.setSpec("{}");
            }

            sku.setSpuId(spu.getId());
            // sku名称 = spu名称+规格值列表
            String name = spu.getName();
            Map<String,String> specMap = JSON.parseObject(sku.getSpec(), Map.class);
            for (String value : specMap.values()) {
                name+=" "+value;
            }


            sku.setName(name);// 名称
            sku.setUpdateTime(date);// 更新日期
            sku.setCategoryName(categoryName); // 所属分类名称
            sku.setCategoryId(category.getId());// 所属分类id
            sku.setCommentNum(0);// 评论数0
            sku.setSaleNum(0); // 销量0
            skuMapper.insert(sku);
        }
    }

    /**
     * 返回全部记录
     * @return
     */
    public List<Spu> findAll() {
        return spuMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Spu> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectAll();
        return new PageResult<Spu>(spus.getTotal(),spus.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Spu> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return spuMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Spu> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectByExample(example);
        return new PageResult<Spu>(spus.getTotal(),spus.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Spu findById(String id) {
        return spuMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param spu
     */
    public void add(Spu spu) {
        spuMapper.insert(spu);
    }

    /**
     * 修改
     * @param spu
     */
    public void update(Spu spu) {
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(String id) {
        spuMapper.deleteByPrimaryKey(id);
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 主键
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andLike("id","%"+searchMap.get("id")+"%");
            }
            // 货号
            if(searchMap.get("sn")!=null && !"".equals(searchMap.get("sn"))){
                criteria.andLike("sn","%"+searchMap.get("sn")+"%");
            }
            // SPU名
            if(searchMap.get("name")!=null && !"".equals(searchMap.get("name"))){
                criteria.andLike("name","%"+searchMap.get("name")+"%");
            }
            // 副标题
            if(searchMap.get("caption")!=null && !"".equals(searchMap.get("caption"))){
                criteria.andLike("caption","%"+searchMap.get("caption")+"%");
            }
            // 图片
            if(searchMap.get("image")!=null && !"".equals(searchMap.get("image"))){
                criteria.andLike("image","%"+searchMap.get("image")+"%");
            }
            // 图片列表
            if(searchMap.get("images")!=null && !"".equals(searchMap.get("images"))){
                criteria.andLike("images","%"+searchMap.get("images")+"%");
            }
            // 售后服务
            if(searchMap.get("saleService")!=null && !"".equals(searchMap.get("saleService"))){
                criteria.andLike("saleService","%"+searchMap.get("saleService")+"%");
            }
            // 介绍
            if(searchMap.get("introduction")!=null && !"".equals(searchMap.get("introduction"))){
                criteria.andLike("introduction","%"+searchMap.get("introduction")+"%");
            }
            // 规格列表
            if(searchMap.get("specItems")!=null && !"".equals(searchMap.get("specItems"))){
                criteria.andLike("specItems","%"+searchMap.get("specItems")+"%");
            }
            // 参数列表
            if(searchMap.get("paraItems")!=null && !"".equals(searchMap.get("paraItems"))){
                criteria.andLike("paraItems","%"+searchMap.get("paraItems")+"%");
            }
            // 是否上架
            if(searchMap.get("isMarketable")!=null && !"".equals(searchMap.get("isMarketable"))){
                criteria.andLike("isMarketable","%"+searchMap.get("isMarketable")+"%");
            }
            // 是否启用规格
            if(searchMap.get("isEnableSpec")!=null && !"".equals(searchMap.get("isEnableSpec"))){
                criteria.andLike("isEnableSpec","%"+searchMap.get("isEnableSpec")+"%");
            }
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andLike("isDelete","%"+searchMap.get("isDelete")+"%");
            }
            // 审核状态
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andLike("status","%"+searchMap.get("status")+"%");
            }

            // 品牌ID
            if(searchMap.get("brandId")!=null ){
                criteria.andEqualTo("brandId",searchMap.get("brandId"));
            }
            // 一级分类
            if(searchMap.get("category1Id")!=null ){
                criteria.andEqualTo("category1Id",searchMap.get("category1Id"));
            }
            // 二级分类
            if(searchMap.get("category2Id")!=null ){
                criteria.andEqualTo("category2Id",searchMap.get("category2Id"));
            }
            // 三级分类
            if(searchMap.get("category3Id")!=null ){
                criteria.andEqualTo("category3Id",searchMap.get("category3Id"));
            }
            // 模板ID
            if(searchMap.get("templateId")!=null ){
                criteria.andEqualTo("templateId",searchMap.get("templateId"));
            }
            // 运费模板id
            if(searchMap.get("freightId")!=null ){
                criteria.andEqualTo("freightId",searchMap.get("freightId"));
            }
            // 销量
            if(searchMap.get("saleNum")!=null ){
                criteria.andEqualTo("saleNum",searchMap.get("saleNum"));
            }
            // 评论数
            if(searchMap.get("commentNum")!=null ){
                criteria.andEqualTo("commentNum",searchMap.get("commentNum"));
            }

        }
        return example;
    }

}
