package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.BrandMapper;
import com.qingcheng.dao.SpecMapper;
import com.qingcheng.pojo.goods.Brand;
import com.qingcheng.service.goods.SkuSearchService;
import javafx.collections.ObservableMap;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

import javax.management.Query;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class SkuSearchServiceImpl implements SkuSearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private SpecMapper specMapper;

    public Map search(Map<String, String> searchMap) {
        //1. 封装查询请求
        SearchRequest searchRequest = new SearchRequest("sku");
        searchRequest.types("doc");

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();// 构建布尔查询构建器

        //1.1搜索关键字
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("name", searchMap.get("keywords"));
        boolQueryBuilder.must(matchQueryBuilder);

        //聚合查询(商品分类)
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("sku_category").field("categoryName");
        searchSourceBuilder.aggregation(termsAggregationBuilder);

        //1.2商品分类过滤
        if (searchMap.get("category")!=null){
            TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("categoryName", searchMap.get("category"));
            boolQueryBuilder.filter(termQueryBuilder);
        }

        // 1.3品牌过滤
        if(searchMap.get("brand")!=null){
            TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("brandName", searchMap.get("brand"));
            boolQueryBuilder.filter(termQueryBuilder);
        }

        // 1.4规格过滤
        for (String key:searchMap.keySet()){
            if (key.startsWith("spec.")){// 如果是规格参数
                TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery(key + ".keyword", searchMap.get(key));
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        // 1.5价格过滤
        if (searchMap.get("price") != null) {
            String priceRange = searchMap.get("price");
            String[] price = priceRange.split("-");
            if (!price[0].equals("0")){// 下线为0，不等于0则为最小值
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price").gte(price[0] + "00");
                boolQueryBuilder.filter(rangeQueryBuilder);
            }
            if (!price[1].equals("*")){// 上线不为*则有上线
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price").lte(price[1] + "00");
                boolQueryBuilder.filter(rangeQueryBuilder);
            }

        }

        // 分页查询
        Integer pageNo = Integer.parseInt(searchMap.get("pageNo"));// 页码
        Integer pageSize = 30;// 每页数据量
        Integer fromIndex = (pageNo - 1) * pageSize; // 起始索引值
        searchSourceBuilder.from(fromIndex);
        searchSourceBuilder.size(pageSize);

        // 排序
        String sort = searchMap.get("sort");
        String sortOrder = searchMap.get("sortOrder");
        if (!"".equals(sort)){
            searchSourceBuilder.sort(sort, SortOrder.valueOf(sortOrder));
        }

        // 高亮显示
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("name").preTags("<font style='color:red'>").postTags("</font>");
        searchSourceBuilder.highlighter(highlightBuilder);


        searchSourceBuilder.query(boolQueryBuilder);

        searchRequest.source(searchSourceBuilder);


        // 2.封装查询结果
        Map resultMap = new HashMap();
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            long totalHits = hits.getTotalHits();
            System.out.println("总记录数：" + totalHits);
            SearchHit[] searchHits = hits.getHits();

            //2.1商品列表
            List<Map<String, Object>> resultList = new ArrayList<Map<String,Object>>();
            for (SearchHit hit : searchHits) {
                Map<String, Object> skuMap = hit.getSourceAsMap();
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                HighlightField name = highlightFields.get("name");
                Text[] fragments = name.fragments();
                skuMap.put("name",fragments[0].toString());//用高亮的内容替换原来的内容
                resultList.add(skuMap);
            }

            resultMap.put("rows",resultList);

            // 2.2分类列表
            Aggregations aggregations = searchResponse.getAggregations();
            Map<String, Aggregation> aggregationMap = aggregations.getAsMap();
            Terms terms = (Terms) aggregationMap.get("sku_category");
            List<? extends Terms.Bucket> buckets = terms.getBuckets();
            List<String> categoryList = new ArrayList();
            for (Terms.Bucket bucket : buckets) {
                categoryList.add(bucket.getKeyAsString());
            }
            resultMap.put("categoryList",categoryList);


            // 获取商品分类名称
            String categoryName = null;// 商品分类名称
            if (searchMap.get("category") == null) {// 如果没有分类条件
                if (categoryList.size() > 0) {
                    categoryName = categoryList.get(0);// 默认提取分类列表的第一个
                }
            } else {
                categoryName = searchMap.get("category");// 取出参数中的分类
            }

            // 2.3品牌列表
            if (searchMap.get("brand") == null) {
                List<Map> brandList = brandMapper.findListByCategoryName(categoryName);// 查询品牌列表
                resultMap.put("brandList", brandList);
            }

            // 2.4规格列表
            List<Map> specList = specMapper.findListByCategoryName(categoryName); // 规格列表
            for (Map spec : specList) {
                String[] options = ((String) spec.get("options")).split(",");// 得到规格选项数组
                spec.put("options",options);
            }

//            for (int i = 0; i < specList.size(); i++) {
//                String [] options = ((String) specList.get(i).get("options")).split(",");//得到规格选项数组
//                specList.get(i).put("options",options);
//            }
            resultMap.put("specList", specList);

            // 总记录数
            resultMap.put("totalCount",totalHits);
            // 总页数
            Long totalPage = (totalHits - 1) / pageSize + 1;
            resultMap.put("totalPage", totalPage);

        } catch (IOException e) {
            e.printStackTrace();
        }


        return resultMap;
    }
}
