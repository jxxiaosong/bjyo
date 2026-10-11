package com.bjyo.framework.shiro.web.filter.captcha;

import com.bjyo.framework.web.service.SliderCaptchaService;
import com.bjyo.common.constant.ShiroConstants;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.Assert.*;

public class LoginCaptchaFilterTest
{
    @Test public void loginRejectsLegacyTextAndMissingSliderProof() throws Exception
    {
        var filter = new CaptchaValidateFilter();
        filter.setSliderCaptchaService(new SliderCaptchaService());
        var request = new MockHttpServletRequest("POST", "/app/login");
        request.setContextPath("/app"); request.setServletPath("/login");
        request.getSession().setAttribute(com.google.code.kaptcha.Constants.KAPTCHA_SESSION_KEY, "1234");
        request.setParameter("validateCode", "1234");
        assertTrue(filter.onPreHandle(request, new MockHttpServletResponse(), null));
        assertEquals(ShiroConstants.CAPTCHA_ERROR, request.getAttribute(ShiroConstants.CURRENT_CAPTCHA));
    }

    @Test public void disabledCaptchaAndLoginPageRemainAccessible() throws Exception
    {
        var filter = new CaptchaValidateFilter(); filter.setCaptchaEnabled(false);
        var request = new MockHttpServletRequest("POST", "/login"); request.setServletPath("/login");
        assertTrue(filter.onPreHandle(request, new MockHttpServletResponse(), null));
        assertNull(request.getAttribute(ShiroConstants.CURRENT_CAPTCHA));
        filter.setCaptchaEnabled(true); request.setMethod("GET");
        assertTrue(filter.onPreHandle(request, new MockHttpServletResponse(), null));
        assertNull(request.getAttribute(ShiroConstants.CURRENT_CAPTCHA));
    }
}
