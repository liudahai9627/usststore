package com.usststore.filters;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.usststore.auth.entiy.UserInfo;
import com.usststore.auth.utils.JwtUtils;
import com.usststore.common.utils.CookieUtils;
import com.usststore.config.FilterProperties;
import com.usststore.config.JwtProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

@Component
@EnableConfigurationProperties({JwtProperties.class, FilterProperties.class})
public class AuthFilter extends ZuulFilter {

    @Autowired
    private JwtProperties prop;

    @Autowired
    private FilterProperties filterProp;

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER-1;
    }

    @Override
    public boolean shouldFilter() {
        //获取上下文
        RequestContext context = RequestContext.getCurrentContext();
        //获取request
        HttpServletRequest request = context.getRequest();
        //获取请求的url路径
        String requestURI = request.getRequestURI();
        //判断是否放行
        Boolean isAllowPath=isAllowPath(requestURI);
        return !isAllowPath;
    }

    private Boolean isAllowPath(String requestURI) {
        for (String allowPath : filterProp.getAllowPaths()) {
            if (requestURI.startsWith(allowPath)){}
            return true;
        }
        return false;
    }

    @Override
    public Object run() throws ZuulException {
        //获取上下文
        RequestContext context = RequestContext.getCurrentContext();
        //获取request
        HttpServletRequest request = context.getRequest();
        //获取token
        Cookie[] cookies = request.getCookies();
        String token = CookieUtils.getCookieValue(request, prop.getCookieName());
        try{
            //解析token
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, prop.getPublicKey());
            //校验权限

        }catch (Exception e){
            //解析token失败，未登陆，拦截
            context.setSendZuulResponse(false);
            //返回状态码
            context.setResponseStatusCode(403);
        }

        return null;
    }
}
