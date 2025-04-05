package com.sdumagicode.backend.config;


import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ResourceUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * Spring MVC 配置
 *
 * @author ronger
 */
@Configuration
public class WebMvcConfigurer extends WebMvcConfigurationSupport {

    private final Logger logger = LoggerFactory.getLogger(WebMvcConfigurer.class);

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();
        // 保留空的字段
        config.setSerializerFeatures(SerializerFeature.WriteMapNullValue,
                //String null -> ""
                SerializerFeature.WriteNullStringAsEmpty);
        // SerializerFeature.WriteNullNumberAsZero);//Number null -> 0
        //关闭循环引用
        config.setSerializerFeatures(SerializerFeature.DisableCircularReferenceDetect);
        // 设置 Long 类型转为 String
        SerializeConfig serializeConfig = new SerializeConfig();
        serializeConfig.put(Long.class, ToStringSerializer.instance);
        serializeConfig.put(Long.TYPE, ToStringSerializer.instance);
        config.setSerializeConfig(serializeConfig);
        converter.setFastJsonConfig(config);
        converter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON));
        converter.setDefaultCharset(Charset.forName("UTF-8"));
        converters.add(0, converter);
    }

    /**
     * 解决跨域问题
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(CorsConfiguration.ALL)
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "DELETE", "PUT", "PATCH");
    }

    /**
     * 访问静态资源
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /**
         * SpringBoot自动配置本身并不会把/swagger-ui.html
         * 这个路径映射到对应的目录META-INF/resources/下面
         * 采用WebMvcConfigurerAdapter将swagger的静态文件进行发布;
         */
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        //将所有/static/** 访问都映射到classpath:/static/ 目录下
        registry.addResourceHandler("/static/**").addResourceLocations(ResourceUtils.CLASSPATH_URL_PREFIX + "/static/");
        super.addResourceHandlers(registry);
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10); // 核心线程数
        taskExecutor.setMaxPoolSize(50); // 最大线程数
        taskExecutor.setQueueCapacity(100); // 队列容量
        taskExecutor.setThreadNamePrefix("mvc-async-"); // 线程名前缀
        taskExecutor.initialize();

        configurer.setTaskExecutor(taskExecutor);
        configurer.setDefaultTimeout(30000); // 设置默认超时时间(毫秒)
    }
}
