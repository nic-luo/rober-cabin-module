package group.rober.runtime.autoconfigure;

import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import group.rober.runtime.kit.ListKit;
import group.rober.runtime.support.BeanPostProcessorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.*;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableConfigurationProperties(RuntimeProperties.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ComponentScan(basePackages = "group.rober.runtime")
public class RuntimeAutoConfiguration extends WebMvcConfigurerAdapter {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private RuntimeProperties properties;

    public RuntimeAutoConfiguration(RuntimeProperties properties) {
        this.properties = properties;
    }


    @Bean("runtimePostProcessor")
    public BeanPostProcessorImpl getBeanPostProcessorImpl(){
        return new BeanPostProcessorImpl();
    }

    /**
     * Druid的StatViewServlet
     * @return
     */
    @Bean
    public ServletRegistrationBean druidServlet() {
        ServletRegistrationBean reg = new ServletRegistrationBean();
        reg.setServlet(new StatViewServlet());
        reg.addUrlMappings("/druid/*");
        reg.addInitParameter("loginUsername", properties.getDruidLoginUsername());
        reg.addInitParameter("loginPassword", properties.getDruidLoginPassword());
        return reg;
    }

    /**
     * Druid的StatFilter
     * @return
     */
    @Bean
    public FilterRegistrationBean filterRegistrationBean() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(new WebStatFilter());
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
        filterRegistrationBean.addInitParameter("profileEnable", "true");
        filterRegistrationBean.addInitParameter("principalCookieName", "USER_COOKIE");
        filterRegistrationBean.addInitParameter("principalSessionName", "USER_SESSION");
        return filterRegistrationBean;
    }
    public void addCorsMappings(CorsRegistry registry) {
        if(properties.isCorsEnable()) {
            registry.addMapping(properties.getPathPattern())
//                    .allowedHeaders(properties.getAllowedHeaders())
                    .allowedMethods(properties.getAllowedMethods())
                    .allowedOrigins(properties.getAllowedOrigins())
                    .exposedHeaders("WWW-Authenticate")
                    .allowCredentials(true);
        }
    }
//    @Bean
//    public CorsFilter corsFilter() {
//        CorsConfiguration corsConfiguration = new CorsConfiguration();
//        corsConfiguration.addAllowedOrigin("*"); // 1
//        corsConfiguration.addAllowedHeader("*"); // 2
//        corsConfiguration.addAllowedMethod("*"); // 3
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", corsConfiguration); // 4
//        return new CorsFilter(source);
//    }


    @Bean
    public FilterRegistrationBean characterEncodingFilterRegistration() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        CharacterEncodingFilter filter = new CharacterEncodingFilter("UTF-8", true);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    @Override
    public void configureDefaultServletHandling(
            DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }


    public StringHttpMessageConverter getStringHttpMessageConverter(){
        StringHttpMessageConverter shmc = new StringHttpMessageConverter(properties.getCharset());
        shmc.setSupportedMediaTypes(ListKit.listOf(new MediaType(MediaType.TEXT_PLAIN,properties.getCharset())));
        return shmc;
    }


    //配置HTTP消息转换器
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        super.configureMessageConverters(converters);
        converters.add(getStringHttpMessageConverter());
    }
}
