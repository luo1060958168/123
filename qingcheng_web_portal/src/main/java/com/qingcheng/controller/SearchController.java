package com.qingcheng.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.SkuSearchService;
import com.qingcheng.util.WebUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class SearchController {

    @Reference
    private SkuSearchService skuSearchService;

    @GetMapping("/search")
    public String search(Model model, @RequestParam Map<String,String> searchMap) throws Exception {
        // 字符集处理
        searchMap = WebUtil.convertCharsetToUTF8(searchMap);
        // 接收页码，如果不传递默认为第一页
        if(searchMap.get("pageNo") == null){
            searchMap.put("pageNo","1");
        }

        // 排序参数容错处理
        if (searchMap.get("sort")== null){
            searchMap.put("sort","");
        }
        if (searchMap.get("sortOrder") == null){
            searchMap.put("sortOrder","DESC");
        }

        Map result = skuSearchService.search(searchMap);
        model.addAttribute("result",result);


        //url处理
        StringBuffer url = new StringBuffer("/search.do?");
        for (String key : searchMap.keySet()) {
            url.append("&"+key+"="+searchMap.get(key));
        }
        model.addAttribute("url",url);
        model.addAttribute("searchMap",searchMap);

        // 获取当前页
        Integer pageNo = Integer.parseInt(searchMap.get("pageNo"));
        model.addAttribute("pageNo",pageNo);

        // 得到总页数
        Long totalPage = (Long) result.get("totalPage");
        int startPage = 1;// 开始页
        int endPage = totalPage.intValue();// 截止页
        if (totalPage > 5){
            // 开始页
            startPage = pageNo - 2;
            if (startPage < 1){
                startPage = 1;
            }
            if (startPage + 4 > totalPage){
                startPage = totalPage.intValue() - 4;
            }
            // 截止页
            endPage = startPage + 4;
        }

        model.addAttribute("startPage",startPage);
        model.addAttribute("endPage",endPage);


        return "search";
    }
}
