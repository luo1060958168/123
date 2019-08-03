package com.qingcheng.controller;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/redirect")
public class RedirectController {

    /**
     * 登录地址返回
     * @param referer
     * @return
     */
    @RequestMapping("/back")
    public String redirect(@RequestHeader (value = "Referer",required = false) String referer){
        if (!StringUtils.isEmpty(referer)){
            return "redirect:"+referer;
        }

        return "redirect:/seckill-index.html";
    }
}
