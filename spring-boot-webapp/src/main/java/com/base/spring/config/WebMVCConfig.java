package com.base.spring.config;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.base.spring.filter.XSSFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.filters.RemoteIpFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.nio.charset.StandardCharsets;
import java.util.List;

// web 配置
//WebMvcConfigurerAdapter 中有更多设置，参考文档
@Configuration
//@EnableWebMvc 不需要这句，已经自动启用
@Slf4j
class WebMVCConfig implements WebMvcConfigurer {

    //private static final log log = LoggerFactory.getLogger(WebMVCConfig.class);

    /**
     * 自定义，并注册   listener 演示
     * 直接用 Bean ，也可以用 ServletListenerRegistrationBean
     *
     * @return
     */
    // 向系统注册一个 RequestContextListener Bean ，这样在其他组件中就可以使用了
    //  CustomUserDetailsService 用到，用于截获 HttpServletRequest
    //  @Autowired
    //  private HttpServletRequest request;
    @Bean
    public RequestContextListener requestContextListener() {

        return new RequestContextListener();
    }


    // 自定义过滤器和监听器
    // http://blog.jobbole.com/97760/
    //http://blog.jobbole.com/97763/
    // 所有过滤器的调用顺序跟添加的顺序相反，过滤器的实现是责任链模式，

    /**
     * 自定义，并注册   listener 演示
     *
     * @return
     */
    @Bean
    protected ServletContextListener listener() {

        return new ServletContextListener() {
            @Override
            public void contextInitialized(ServletContextEvent sce) {

            }

            @Override
            public void contextDestroyed(ServletContextEvent sce) {
                log.info("ServletContext destroyed");
            }

        };
    }


    /**
     * 自定义，并注册 filter
     * 将代理服务器发来的请求包含的IP地址转换成真正的用户IP
     *
     * @return
     */
    @Bean
    public FilterRegistrationBean remoteIpFilter() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new RemoteIpFilter());
        registration.addUrlPatterns("/*");
        registration.setName("RemoteIpFilter");
        log.info("RemoteIpFilter initialized");
        return registration;
    }

    /**
     * 自定义，并注册 filter (通过 FilterRegistrationBean)，增加 XSSFilter
     *
     * @return
     */
    @Bean
    public FilterRegistrationBean someFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new XSSFilter());
        registration.addUrlPatterns("/*");
        registration.setName("XSSFilter");
        log.info("XSSFilter initialized");
        return registration;
    }


    /**
     * 注册一个拦截器
     * 拦截器只在 spring web 中使用
     * filter 可以在 java web 中使用，范围广
     *
     * @param registry
     */
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(new HandlerInterceptor()).addPathPatterns("/person/save/*");
//    }

    /**
     * Spring 3.2 及以上版本自动开启检测URL后缀,设置Response content-type功能, 如果不手动关闭这个功能,当url后缀与accept头不一致时,
     * Response的content-type将会和request的accept不一致,导致报 406 错误
     * 例如返回类型为 JSON 的 @ResponseBody API, 必须将请求URL后缀改为.json，以便和 accept头(application/json)相匹配，否则返回 406 错误。
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {

        configurer.
                favorPathExtension(false). //关闭URL后缀检测
                favorParameter(true).
                parameterName("mediaType").
                ignoreAcceptHeader(true).
                useRegisteredExtensionsOnly(false).
                mediaType("xml", MediaType.APPLICATION_XML).
                mediaType("json", MediaType.APPLICATION_JSON).
                defaultContentType(MediaType.APPLICATION_JSON);//如果没有对应的后缀名，返回信息默认以 json 格式返回

    }

    /**
     * PathMatchConfigurer 函数让开发人员可以根据需求定制URL路径的匹配规则。
     *
     * @param configurer
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        /**
         * spring mvc 默认忽略 url 中点"."后面的部分，如
         * http://localhost:8080/abc.mm  会直接匹配为
         * http://localhost:8080/abc 忽略了 mm
         * 如果不想忽略，设置 setUseSuffixPatternMatch(false)
         */

        configurer.setUseSuffixPatternMatch(false);
    }

    /**
     * 自定义转换器
     * -
     * Configure the HttpMessageConverters to use for reading or writing to the body of the request or response.
     * - spring mvc message converters
     *
     * @param converters
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {

        //替换 jackson 为 fastJson
        //fastJson 有很多符合国人的使用习惯
        converters.add(getFastJsonConverter());

        //其他转换器
//        List<HttpMessageConverter<?>> messageConverters = new ArrayList();
//        messageConverters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
//        messageConverters.add(new FormHttpMessageConverter());
//        messageConverters.add(new ByteArrayHttpMessageConverter());
//        converters.addAll(messageConverters);
    }


    /**
     * 自定义 一个 json 转换器
     * 详见 fast json 官方文档
     *
     * @return
     */
    private HttpMessageConverter getFastJsonConverter() {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        //自定义配置...
        com.alibaba.fastjson.support.config.FastJsonConfig config = new com.alibaba.fastjson.support.config.FastJsonConfig();
        SerializerFeature[] features = {
                SerializerFeature.DisableCircularReferenceDetect,
                SerializerFeature.WriteDateUseDateFormat,
                SerializerFeature.PrettyFormat};
        config.setSerializerFeatures(features);
        config.setDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        config.setCharset(StandardCharsets.UTF_8);
        converter.setFastJsonConfig(config);
        return converter;

    }

}