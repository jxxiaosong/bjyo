package com.bjyo.framework.shiro.service;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.bjyo.common.constant.Constants;
import com.bjyo.common.constant.ShiroConstants;
import com.bjyo.common.core.domain.entity.SysUser;
import com.bjyo.common.exception.user.UserPasswordNotMatchException;
import com.bjyo.common.exception.user.UserPasswordRetryLimitExceedException;
import com.bjyo.common.utils.MessageUtils;
import com.bjyo.common.utils.security.Md5Utils;
import com.bjyo.framework.manager.AsyncManager;
import com.bjyo.framework.manager.factory.AsyncFactory;
import jakarta.annotation.PostConstruct;

/**
 * 登录密码方法
 * 
 * @author bjyo
 */
@Component
public class SysPasswordService
{
    @Autowired
    private CacheManager cacheManager;

    private Cache<String, AtomicInteger> loginRecordCache;

    @Value(value = "${user.password.maxRetryCount}")
    private String maxRetryCount;

    @PostConstruct
    public void init()
    {
        loginRecordCache = cacheManager.getCache(ShiroConstants.LOGIN_RECORD_CACHE);
    }

    public void validate(SysUser user, String password)
    {
        String loginName = user.getLoginName();

        AtomicInteger retryCount = loginRecordCache.get(loginName);

        if (retryCount == null)
        {
            retryCount = new AtomicInteger(0);
            loginRecordCache.put(loginName, retryCount);
        }
        if (retryCount.incrementAndGet() > Integer.valueOf(maxRetryCount).intValue())
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(loginName, Constants.LOGIN_FAIL, MessageUtils.message("user.password.retry.limit.exceed", maxRetryCount)));
            throw new UserPasswordRetryLimitExceedException(Integer.valueOf(maxRetryCount).intValue());
        }

        if (!matches(user, password))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(loginName, Constants.LOGIN_FAIL, MessageUtils.message("user.password.retry.limit.count", retryCount)));
            loginRecordCache.put(loginName, retryCount);
            throw new UserPasswordNotMatchException();
        }
        else
        {
            clearLoginRecordCache(loginName);
        }
    }

    public boolean matches(SysUser user, String newPassword)
    {
        return user.getPassword().equals(encryptPassword(user.getLoginName(), newPassword, user.getSalt()));
    }

    public void clearLoginRecordCache(String loginName)
    {
        loginRecordCache.remove(loginName);
    }

    public String encryptPassword(String loginName, String password, String salt)
    {
        return Md5Utils.hash(loginName + password + salt);
    }
}
