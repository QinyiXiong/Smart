package com.sdumagicode.backend.config;

import com.sdumagicode.backend.auth.BaseHashedCredentialsMatcher;
import com.sdumagicode.backend.auth.JwtFilter;
import com.sdumagicode.backend.auth.JwtRealm;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.servlet.Filter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author ronger
 */
@Configuration
public class ShiroConfig {

    @Bean
    @DependsOn("securityManager")
    public ShiroFilterFactoryBean shirFilter(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        //添加自定义的过滤器
        LinkedHashMap<String, Filter> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("jwt", new JwtFilter());
        shiroFilterFactoryBean.setFilters(linkedHashMap);

        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<String, String>();
        filterChainDefinitionMap.put("/actuator/**", "anon");
        filterChainDefinitionMap.put("/api/v1/console/**", "anon");
        filterChainDefinitionMap.put("/api/v1/article/detail/**", "anon");
        filterChainDefinitionMap.put("/api/v1/topic/**", "anon");
        filterChainDefinitionMap.put("/api/v1/user/**", "anon");
        filterChainDefinitionMap.put("/api/v1/article/*/comments", "anon");
        filterChainDefinitionMap.put("/api/v1/rule/currency/**", "anon");
        filterChainDefinitionMap.put("/api/v1/lucene/**", "anon");
        filterChainDefinitionMap.put("/api/v1/open-data/**", "anon");
        filterChainDefinitionMap.put("/api/v1/auth/login/**", "anon");
        filterChainDefinitionMap.put("/api/v1/auth/logout/**", "anon");
        filterChainDefinitionMap.put("/api/v1/auth/refresh-token/**", "anon");
        filterChainDefinitionMap.put("/api/v1/sse/**", "anon");
        filterChainDefinitionMap.put("/**", "jwt");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);

        return shiroFilterFactoryBean;
    }

    @Bean
    public JwtRealm baseShiroRealm() {
        JwtRealm shiroRealm = new JwtRealm();
        shiroRealm.setCredentialsMatcher(new BaseHashedCredentialsMatcher());
        return shiroRealm;
    }

    /***
     * 配置安全管理器
     * */
    @Bean
    public SecurityManager securityManager() {
        DefaultWebSecurityManager defaultWebSecurityManager = new DefaultWebSecurityManager();
        defaultWebSecurityManager.setRealm(baseShiroRealm());
        defaultWebSecurityManager.setRememberMeManager(null);

        // 显式绑定SecurityManager到线程上下文
        SecurityUtils.setSecurityManager(defaultWebSecurityManager);
        return defaultWebSecurityManager;
    }

    /**
     * 开启shiro aop注解支持.
     * 使用代理方式;所以需要开启代码支持;
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor = new AuthorizationAttributeSourceAdvisor();
        authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
        return authorizationAttributeSourceAdvisor;
    }

    /**
     * Shiro生命周期处理器
     */
    @Bean
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    @Bean
    @DependsOn({"lifecycleBeanPostProcessor"})
    public DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator = new DefaultAdvisorAutoProxyCreator();
        advisorAutoProxyCreator.setProxyTargetClass(true);
        return advisorAutoProxyCreator;
    }

    @Bean
    public FilterRegistrationBean<DelegatingFilterProxy> shiroFilterRegistration() {
        FilterRegistrationBean<DelegatingFilterProxy> registration = new FilterRegistrationBean<>();
        registration.setFilter(new DelegatingFilterProxy("shirFilter"));
        registration.addInitParameter("targetFilterLifecycle", "true");
        registration.setAsyncSupported(true);  // 启用异步支持
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

}
