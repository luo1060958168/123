package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.CategoryReportMapper;
import com.qingcheng.pojo.order.CategoryReport;
import com.qingcheng.service.order.CategoryReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service(interfaceClass = CategoryReportService.class)
@Transactional
public class CategoryReportServiceImpl implements CategoryReportService {

    @Autowired
    private CategoryReportMapper categoryReportMapper;

    /**
     * 获取分类销售报表
     * @param date
     * @return
     */
    @Override
    public List<CategoryReport> getCategoryReport(LocalDate date) {
        return categoryReportMapper.getCategoryReport(date);
    }

    /**
     * 生成销售报表
     */
    @Override
    public void createData() {
        LocalDate localDate = LocalDate.now().minusDays(1);
        List<CategoryReport> categoryReportList = categoryReportMapper.getCategoryReport(localDate);
        for (CategoryReport categoryReport : categoryReportList) {
            categoryReportMapper.insert(categoryReport);
        }
    }

    @Override
    public List<Map> category1Count(String date1, String date2) {
        return categoryReportMapper.category1Count(date1,date2);
    }
}
