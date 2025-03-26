package com.sdumagicode.backend.controller.admin;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.dto.TransactionRecordDTO;
import com.sdumagicode.backend.entity.CurrencyRule;
import com.sdumagicode.backend.service.CurrencyRuleService;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Created on 2022/3/6 18:26.
 *
 * @author ronger
 * @email ronger-x@outlook.com
 */
@RestController
@RequestMapping("/api/v1/admin/rule/currency")
@RequiresRoles(value = {"blog_admin", "admin"}, logical = Logical.OR)
public class AdminCurrencyRuleController {
    @Resource
    private CurrencyRuleService currencyRuleService;

    @GetMapping("/list")
    public GlobalResult<PageInfo<TransactionRecordDTO>> list(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer rows, HttpServletRequest request) {
        PageHelper.startPage(page, rows);
        List<CurrencyRule> list = currencyRuleService.findAll();
        PageInfo<TransactionRecordDTO> pageInfo = new PageInfo(list);
        return GlobalResultGenerator.genSuccessResult(pageInfo);
    }

}
