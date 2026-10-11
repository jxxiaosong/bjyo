package com.bjyo.framework.web.service;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.LongSupplier;
import java.io.Serializable;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

/** One-use slider challenge and proof bound to the existing Shiro HTTP session. */
@Service
public class SliderCaptchaService
{
    private static final String STATE_KEY = "bjyo.slider.state";
    private static final int MAX_PENDING = 8;
    private static final int IMAGE_WIDTH = 320;
    private static final int IMAGE_HEIGHT = 160;
    private static final int PIECE_SIZE = 52;
    private static final int VERIFY_TOLERANCE = 7;
    private static final long MIN_VERIFY_MILLIS = 250L;
    private static final long MAX_VERIFY_MILLIS = 120_000L;
    private static final String NOISE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random;
    private final LongSupplier clock;

    public SliderCaptchaService() { this(new SecureRandom(), System::currentTimeMillis); }
    SliderCaptchaService(SecureRandom random, LongSupplier clock) { this.random = random; this.clock = clock; }

    private record Challenge(String uuid, int targetX, long issuedAt) implements Serializable { }
    private record Proof(String uuid, String token, long issuedAt) implements Serializable { }

    private static class State implements Serializable
    {
        private static final long serialVersionUID = 1L;
        final transient Map<String, Challenge> challenges = new LinkedHashMap<>();
        final transient Map<String, Proof> proofs = new LinkedHashMap<>();

        // Shiro persists authenticated sessions. Restoring an older snapshot must never revive a spent proof.
        private Object readResolve() { return new State(); }
    }

    private State state(HttpSession session)
    {
        Object stored = session.getAttribute(STATE_KEY);
        State state = stored instanceof State ? (State) stored : new State();
        long now = clock.getAsLong();
        state.challenges.values().removeIf(value -> now - value.issuedAt() > MAX_VERIFY_MILLIS);
        state.proofs.values().removeIf(value -> now - value.issuedAt() > MAX_VERIFY_MILLIS);
        return state;
    }

    private <T> void store(Map<String, T> values, String uuid, T value)
    {
        while (values.size() >= MAX_PENDING) values.remove(values.keySet().iterator().next());
        values.put(uuid, value);
    }

    // Synchronize on the singleton: Shiro may wrap the same session in different request objects.
    public synchronized Map<String, Object> createChallenge(HttpSession session)
    {
        State state = state(session);
        int targetX = 82 + random.nextInt(IMAGE_WIDTH - PIECE_SIZE - 100);
        int targetY = 26 + random.nextInt(IMAGE_HEIGHT - PIECE_SIZE - 38);
        BufferedImage source = createBackground();
        Area shape = createPieceShape(random.nextInt(4));
        String uuid = "slider_" + java.util.UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> challenge = new LinkedHashMap<>();
        challenge.put("uuid", uuid);
        challenge.put("backgroundImage", toDataUrl(createBackgroundWithHole(source, shape, targetX, targetY)));
        challenge.put("pieceImage", toDataUrl(createPiece(source, shape, targetX, targetY)));
        challenge.put("imageWidth", IMAGE_WIDTH);
        challenge.put("imageHeight", IMAGE_HEIGHT);
        challenge.put("pieceSize", PIECE_SIZE);
        challenge.put("pieceY", targetY);
        store(state.challenges, uuid, new Challenge(uuid, targetX, clock.getAsLong()));
        session.setAttribute(STATE_KEY, state);
        return challenge;
    }

    public synchronized String verify(HttpSession session, String uuid, Integer offset)
    {
        if (session == null || !validUuid(uuid)) return null;
        State state = state(session);
        // Consume only the requested UUID; late requests cannot erase newer challenges or proofs.
        Challenge challenge = state.challenges.remove(uuid);
        session.setAttribute(STATE_KEY, state);
        if (challenge == null || offset == null || offset < 0 || offset > IMAGE_WIDTH - PIECE_SIZE) return null;
        long elapsed = clock.getAsLong() - challenge.issuedAt();
        if (elapsed < MIN_VERIFY_MILLIS || elapsed > MAX_VERIFY_MILLIS
                || Math.abs(offset - challenge.targetX()) > VERIFY_TOLERANCE) return null;
        byte[] bytes = new byte[24]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        store(state.proofs, uuid, new Proof(uuid, token, clock.getAsLong()));
        session.setAttribute(STATE_KEY, state);
        return token;
    }

    public synchronized boolean consume(HttpSession session, String uuid, String token)
    {
        if (session == null || !validUuid(uuid)) return false;
        State state = state(session);
        Proof proof = state.proofs.remove(uuid);
        session.setAttribute(STATE_KEY, state);
        if (proof == null || token == null || !token.matches("[A-Za-z0-9_-]{32}")) return false;
        long elapsed = clock.getAsLong() - proof.issuedAt();
        return elapsed >= 0 && elapsed <= MAX_VERIFY_MILLIS && MessageDigest.isEqual(
                proof.token().getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }

    private boolean validUuid(String uuid) { return uuid != null && uuid.matches("slider_[0-9a-f]{32}"); }

    private BufferedImage createBackground()
    {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        configure(graphics);
        BufferedImage photo = loadBackgroundPhoto();
        if (photo != null)
        {
            int cropWidth = Math.min(800, photo.getWidth());
            int cropHeight = Math.min(400, photo.getHeight());
            int sourceX = photo.getWidth() == cropWidth ? 0 : random.nextInt(photo.getWidth() - cropWidth);
            int sourceY = photo.getHeight() == cropHeight ? 0 : random.nextInt(photo.getHeight() - cropHeight);
            graphics.drawImage(photo, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, sourceX, sourceY,
                    sourceX + cropWidth, sourceY + cropHeight, null);
            graphics.setColor(new Color(13, 92, 70, 34));
            graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        }
        else
        {
            paintFallbackGradient(graphics);
        }
        for (int index = 0; index < 18; index++)
        {
            int red = 24 + random.nextInt(190);
            int green = 40 + random.nextInt(190);
            int blue = 38 + random.nextInt(190);
            graphics.setColor(new Color(red, green, blue, 28 + random.nextInt(58)));
            int x = random.nextInt(IMAGE_WIDTH);
            int y = random.nextInt(IMAGE_HEIGHT);
            int size = 10 + random.nextInt(45);
            if ((index & 1) == 0)
            {
                graphics.fillOval(x, y, size, size);
            }
            else
            {
                Polygon polygon = new Polygon();
                polygon.addPoint(x, y);
                polygon.addPoint(x + size, y + random.nextInt(20));
                polygon.addPoint(x + random.nextInt(size + 1), y + size);
                graphics.fillPolygon(polygon);
            }
        }
        graphics.setStroke(new BasicStroke(1f + random.nextFloat() * 2f));
        for (int index = 0; index < 14; index++)
        {
            graphics.setColor(new Color(random.nextInt(120), 45 + random.nextInt(120),
                    45 + random.nextInt(120), 55 + random.nextInt(80)));
            graphics.drawLine(random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT),
                    random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT));
        }
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        for (int index = 0; index < 9; index++)
        {
            graphics.setColor(new Color(255, 255, 255, 50 + random.nextInt(85)));
            char character = NOISE_CHARS.charAt(random.nextInt(NOISE_CHARS.length()));
            graphics.drawString(String.valueOf(character), random.nextInt(IMAGE_WIDTH - 16),
                    18 + random.nextInt(IMAGE_HEIGHT - 22));
        }
        graphics.dispose();
        return image;
    }

    private BufferedImage loadBackgroundPhoto()
    {
        try (InputStream input = SliderCaptchaService.class.getResourceAsStream("/captcha/slider-background.jpg"))
        {
            return input == null ? null : ImageIO.read(input);
        }
        catch (IOException exception)
        {
            return null;
        }
    }

    private void paintFallbackGradient(Graphics2D graphics)
    {
        int hue = random.nextInt(80);
        Color start = new Color(32 + hue / 3, 112 + hue, 92 + hue / 2);
        Color end = new Color(190 + hue / 2, 222, 207 + hue / 3);
        graphics.setPaint(new GradientPaint(0, 0, start, IMAGE_WIDTH, IMAGE_HEIGHT, end));
        graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    private Area createPieceShape(int variant)
    {
        Area shape = new Area(new RoundRectangle2D.Double(6, 6, PIECE_SIZE - 12, PIECE_SIZE - 12, 8, 8));
        Area left = new Area(new Ellipse2D.Double(0, PIECE_SIZE / 2.0 - 8, 16, 16));
        Area right = new Area(new Ellipse2D.Double(PIECE_SIZE - 16, PIECE_SIZE / 2.0 - 8, 16, 16));
        Area top = new Area(new Ellipse2D.Double(PIECE_SIZE / 2.0 - 8, 0, 16, 16));
        Area bottom = new Area(new Ellipse2D.Double(PIECE_SIZE / 2.0 - 8, PIECE_SIZE - 16, 16, 16));
        if (variant == 0)
        {
            shape.add(right);
            shape.subtract(left);
        }
        else if (variant == 1)
        {
            shape.add(top);
            shape.subtract(bottom);
        }
        else if (variant == 2)
        {
            shape.add(right);
            shape.subtract(top);
        }
        else
        {
            shape.add(bottom);
            shape.subtract(left);
        }
        return shape;
    }

    private BufferedImage createPiece(BufferedImage source, Area shape, int targetX, int targetY)
    {
        BufferedImage piece = new BufferedImage(PIECE_SIZE, PIECE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = piece.createGraphics();
        configure(graphics);
        graphics.setClip(shape);
        graphics.drawImage(source, -targetX, -targetY, null);
        graphics.setClip(null);
        graphics.setColor(new Color(255, 255, 255, 220));
        graphics.setStroke(new BasicStroke(2f));
        graphics.draw(shape);
        graphics.dispose();
        return piece;
    }

    private BufferedImage createBackgroundWithHole(BufferedImage source, Area shape, int targetX, int targetY)
    {
        BufferedImage background = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = background.createGraphics();
        configure(graphics);
        graphics.drawImage(source, 0, 0, null);
        Area targetShape = new Area(shape);
        targetShape.transform(AffineTransform.getTranslateInstance(targetX, targetY));
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.58f));
        graphics.setColor(new Color(9, 38, 31));
        graphics.fill(targetShape);
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setColor(new Color(255, 255, 255, 210));
        graphics.setStroke(new BasicStroke(2f));
        graphics.draw(targetShape);
        graphics.dispose();
        return background;
    }

    private void configure(Graphics2D graphics)
    {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private String toDataUrl(BufferedImage image)
    {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream())
        {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("滑块验证码生成失败", exception);
        }
    }
}
