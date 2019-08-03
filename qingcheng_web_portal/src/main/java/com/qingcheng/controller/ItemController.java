package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.qingcheng.pojo.goods.Goods;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.goods.Spu;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SpuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/item")
public class ItemController {

    @Reference
    private SpuService spuService;

    @Value("${pagePath}")
    private String pagePath;

    @Autowired
    private TemplateEngine templateEngine;

    @Reference
    private CategoryService categoryService;

    /**
     * 生成详情页
     * @param id
     */
    @GetMapping("/createPage")
    public void createPage(String id){
        // 获取商品数据
        Goods goods = spuService.findGoodsById(id);//商品
        Spu spu = goods.getSpu();
        List<Sku> skuList = goods.getSkuList();
        // 查询商品分类
        List<String> categoryList = new ArrayList<String>();
        categoryList.add(categoryService.findById(spu.getCategory1Id()).getName());// 一级分类
        categoryList.add(categoryService.findById(spu.getCategory2Id()).getName());// 二级分类
        categoryList.add(categoryService.findById(spu.getCategory3Id()).getName());// 三级分类


        //生成urlMap
        Map urlMap = new HashMap();
        for (Sku sku : skuList) {
            if ("1".equals(sku.getStatus())){// 状态为1才添加地址
                //对规格json字符串进行排序
                String specJson = JSON.toJSONString(JSON.parseObject(sku.getSpec()), SerializerFeature.MapSortField);
                urlMap.put(specJson,sku.getId()+".html");
            }
        }

        for (Sku sku : skuList) {

            // 创建thymeleaf上下文
            Context context = new Context();
            // 为每一个sku创建一个页面
            Map<String, Object> dataModel = new HashMap();
            dataModel.put("spu",spu);
            dataModel.put("sku",sku);
            dataModel.put("categoryList",categoryList);// 商品分类面包屑
            dataModel.put("skuImages",sku.getImages().split(","));// sku图片列表
            dataModel.put("spuImages",spu.getImages().split(","));// spu图片列表
            Map paraItems = JSON.parseObject(spu.getParaItems());//spu参数列表
            dataModel.put("paraItems",paraItems);
            Map specItems = JSON.parseObject(sku.getSpec());// 当前sku规格
            dataModel.put("specItems", specItems);
            Map<String,List> specMap = (Map) JSON.parse(spu.getSpecItems());
            for (String key : specMap.keySet()) {// 循环规格列表
                List<String> list = specMap.get(key);
                List<Map> mapList = new ArrayList<Map>();
                for (String value : list) {//循环规格值
                    Map map = new HashMap();
                    map.put("option",value);
                    if (specItems.get(key).equals(value)){// 判断此规格组合是否是当前sku的标记为选中状态
                        map.put("checked",true);
                    }else {
                        map.put("checked",false);
                    }
                    Map spec = JSON.parseObject(sku.getSpec());//当前sku规格
                    spec.put(key,value);
                    String newSpecJson = JSON.toJSONString(spec,SerializerFeature.MapSortField);
                    map.put("url",urlMap.get(newSpecJson));

                    mapList.add(map);
                }

                specMap.put(key,mapList); //用新的集合覆盖原来的集合
            }
            dataModel.put("specMap", specMap);// 规格面板
            context.setVariables(dataModel);

            // 准备文件
            File dir = new File(pagePath);
            if (!dir.exists()){
                dir.mkdirs();
            }

            File dest = new File(dir, sku.getId() + ".html");

            // 生成页面
            try {
                PrintWriter printWriter = new PrintWriter(dest, "UTF-8");
                templateEngine.process("item",context,printWriter);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
    }
}
