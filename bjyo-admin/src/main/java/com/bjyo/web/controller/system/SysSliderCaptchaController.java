package com.bjyo.web.controller.system;

import com.bjyo.common.core.domain.AjaxResult;
import com.bjyo.framework.web.service.SliderCaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SysSliderCaptchaController
{
    private final SliderCaptchaService service;
    private final boolean enabled;

    public SysSliderCaptchaController(SliderCaptchaService service,
            @Value("${shiro.user.captchaEnabled:true}") boolean enabled)
    {
        this.service = service;
        this.enabled = enabled;
    }

    @GetMapping("/captcha/slider")
    public AjaxResult challenge(HttpServletRequest request, HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "no-store");
        AjaxResult result = AjaxResult.success().put("captchaEnabled", enabled);
        return enabled ? result.put("data", service.createChallenge(request.getSession())) : result;
    }

    public record Verification(String uuid, Integer offset) { }

    @PostMapping("/captcha/slider/verify")
    public AjaxResult verify(@RequestBody Verification input, HttpServletRequest request, HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "no-store");
        if (!enabled) return AjaxResult.error("验证码配置已更新，请刷新登录页");
        var session = request.getSession(false);
        String token = session == null || input == null ? null : service.verify(session, input.uuid(), input.offset());
        return token == null ? AjaxResult.error("拖动位置不正确或验证已过期，请重试") : AjaxResult.success("验证通过", token);
    }
}
