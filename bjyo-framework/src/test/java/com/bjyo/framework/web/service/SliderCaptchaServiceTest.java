package com.bjyo.framework.web.service;

import java.io.ByteArrayInputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;
import static org.junit.Assert.*;

public class SliderCaptchaServiceTest
{
    private final AtomicLong now = new AtomicLong(1_000_000);
    // Fixed random inputs give a hand-derived target of x=82 without reading a secret from the response.
    private final SliderCaptchaService service = new SliderCaptchaService(new SecureRandom() {
        @Override public int nextInt(int bound) { return 0; }
    }, now::get);
    private final MockHttpSession session = new MockHttpSession();

    private String challenge() { return (String) service.createChallenge(session).get("uuid"); }
    private String solve(String uuid) { now.addAndGet(500); return service.verify(session, uuid, 82); }

    @Test public void returnsImagesWithoutDisclosingAnswer() throws Exception
    {
        Map<String, Object> result = service.createChallenge(session);
        assertTrue(((String) result.get("uuid")).matches("slider_[0-9a-f]{32}"));
        assertFalse(result.containsKey("targetX"));
        var background = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(
                ((String) result.get("backgroundImage")).split(",")[1])));
        var piece = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(
                ((String) result.get("pieceImage")).split(",")[1])));
        assertEquals(320, background.getWidth()); assertEquals(160, background.getHeight());
        assertEquals(52, piece.getWidth()); assertTrue(piece.getColorModel().hasAlpha());
        String fixture = System.getProperty("slider.fixture");
        if (fixture != null) java.nio.file.Files.writeString(java.nio.file.Path.of(fixture),
                com.alibaba.fastjson.JSON.toJSONString(result));
    }

    @Test public void proofIsBoundToSessionAndUsableOnce()
    {
        String uuid = challenge(), token = solve(uuid);
        assertNotNull(token);
        assertFalse(service.consume(new MockHttpSession(), uuid, token));
        assertTrue(service.consume(session, uuid, token));
        assertFalse(service.consume(session, uuid, token));
    }

    @Test public void wrongPositionConsumesChallenge()
    {
        String uuid = challenge(); now.addAndGet(500);
        assertNull(service.verify(session, uuid, 0));
        assertNull(service.verify(session, uuid, 82));
    }

    @Test public void staleVerificationCannotInvalidateRefreshedChallengeOrProof()
    {
        String oldUuid = challenge(), uuid = challenge(); now.addAndGet(500);
        assertNull(service.verify(session, oldUuid, 0));
        String token = service.verify(session, uuid, 82);
        assertNotNull(token);
        assertNull(service.verify(session, uuid, 82));
        assertTrue(service.consume(session, uuid, token));
    }

    @Test public void staleLoginCannotConsumeNewerProof()
    {
        String oldUuid = challenge(), oldToken = solve(oldUuid);
        String uuid = challenge(), token = solve(uuid);
        assertTrue(service.consume(session, oldUuid, oldToken));
        assertTrue(service.consume(session, uuid, token));
    }

    @Test public void rejectsTooFastAndExpiredChallenges()
    {
        assertNull(service.verify(session, challenge(), 82));
        String uuid = challenge(); now.addAndGet(120_001);
        assertNull(service.verify(session, uuid, 82));
    }

    @Test public void lateChallengeRequestDoesNotInvalidateVerifiedProof()
    {
        String uuid = challenge(), token = solve(uuid); challenge();
        assertTrue(service.consume(session, uuid, token));
    }

    @Test public void expiredOrTamperedProofCannotLogin()
    {
        String uuid = challenge(), token = solve(uuid); now.addAndGet(120_001);
        assertFalse(service.consume(session, uuid, token));
        uuid = challenge(); token = solve(uuid);
        assertFalse(service.consume(session, uuid, "x".repeat(32)));
        assertFalse(service.consume(session, uuid, token));
        assertFalse(service.consume(session, null, null));
    }

    @Test public void concurrentLoginOnlyConsumesProofOnce() throws Exception
    {
        String uuid = challenge(), token = solve(uuid);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var one = pool.submit(() -> service.consume(session, uuid, token));
            var two = pool.submit(() -> service.consume(session, uuid, token));
            assertNotEquals(one.get(), two.get());
        } finally { pool.shutdownNow(); }
    }

    @Test public void loginFilterConsumesSuccessfulProofAndRejectsReplay() throws Exception
    {
        String uuid = challenge(), token = solve(uuid);
        var filter = new com.bjyo.framework.shiro.web.filter.captcha.CaptchaValidateFilter();
        filter.setSliderCaptchaService(service);
        var request = new org.springframework.mock.web.MockHttpServletRequest("POST", "/login");
        request.setServletPath("/login"); request.setSession(session);
        request.setParameter("uuid", uuid); request.setParameter("validateCode", token);
        assertTrue(filter.onPreHandle(request, new org.springframework.mock.web.MockHttpServletResponse(), null));
        assertNull(request.getAttribute(com.bjyo.common.constant.ShiroConstants.CURRENT_CAPTCHA));
        filter.onPreHandle(request, new org.springframework.mock.web.MockHttpServletResponse(), null);
        assertEquals(com.bjyo.common.constant.ShiroConstants.CAPTCHA_ERROR,
                request.getAttribute(com.bjyo.common.constant.ShiroConstants.CURRENT_CAPTCHA));
    }

    @Test public void restoringShiroSessionDoesNotRestoreCaptchaChallengesOrProofs() throws Exception
    {
        String uuid = challenge(), token = solve(uuid), pendingUuid = challenge();
        now.addAndGet(500);
        var onlineSession = new com.bjyo.common.core.session.OnlineSession();
        var names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement(); onlineSession.setAttribute(name, session.getAttribute(name));
        }
        var bytes = new java.io.ByteArrayOutputStream();
        try (var output = new java.io.ObjectOutputStream(bytes)) { output.writeObject(onlineSession); }
        assertTrue(service.consume(session, uuid, token));
        var online = new com.bjyo.system.domain.SysUserOnline();
        online.setSessionData(bytes.toByteArray());
        var restored = new com.bjyo.framework.shiro.session.OnlineSessionFactory().createSession(online);
        var restoredHttpSession = new MockHttpSession();
        for (Object key : restored.getAttributeKeys()) {
            restoredHttpSession.setAttribute((String) key, restored.getAttribute(key));
        }
        assertFalse(service.consume(restoredHttpSession, uuid, token));
        assertNull(service.verify(restoredHttpSession, pendingUuid, 82));
    }
}
