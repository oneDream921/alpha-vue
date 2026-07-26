package io.github.onedream921.alphavue.modules.auth.service;

import io.github.onedream921.alphavue.modules.auth.config.CaptchaProperties;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * 验证码服务
 */
@Service
public class CaptchaService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final CaptchaProperties properties;
    private final CaptchaStore store;

    public CaptchaService(CaptchaProperties properties, CaptchaStore store) {
        this.properties = properties;
        this.store = store;
    }

    /**
     * 创建验证码挑战；验证码关闭时返回禁用状态
     */
    public CaptchaResponse create() {
        if (!properties.isCaptchaEnabled()) {
            return new CaptchaResponse(false, null, null);
        }
        String id = UUID.randomUUID().toString();
        String code = "%04d".formatted(RANDOM.nextInt(10_000));
        store.put(id, code, properties.getCaptchaTtl());
        return new CaptchaResponse(true, id, render(code));
    }

    /**
     * 校验提交的验证码，校验失败时抛出统一认证错误
     */
    public void validate(String id, String submittedCode) {
        if (!properties.isCaptchaEnabled()) {
            return;
        }
        String expected = id == null ? null : store.consume(id);
        if (expected == null || submittedCode == null || !expected.equalsIgnoreCase(submittedCode.trim())) {
            throw new BusinessException(401, PublicErrorMessage.INVALID_CREDENTIALS);
        }
    }

    private static String render(String code) {
        BufferedImage image = new BufferedImage(120, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(242 + RANDOM.nextInt(10), 245 + RANDOM.nextInt(8), 250));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setStroke(new BasicStroke(1.2f));
            for (int index = 0; index < 7; index++) {
                graphics.setColor(randomColor(120, 210));
                graphics.drawLine(RANDOM.nextInt(120), RANDOM.nextInt(40), RANDOM.nextInt(120), RANDOM.nextInt(40));
            }
            for (int index = 0; index < 90; index++) {
                graphics.setColor(randomColor(160, 230));
                graphics.fillRect(RANDOM.nextInt(120), RANDOM.nextInt(40), 1, 1);
            }
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
            for (int index = 0; index < code.length(); index++) {
                Graphics2D character = (Graphics2D) graphics.create();
                try {
                    character.setColor(randomColor(20, 100));
                    character.rotate(Math.toRadians(RANDOM.nextInt(31) - 15), 16 + index * 24, 23);
                    character.drawString(String.valueOf(code.charAt(index)), 12 + index * 25 + RANDOM.nextInt(5), 29 + RANDOM.nextInt(5) - 2);
                } finally {
                    character.dispose();
                }
            }
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to render captcha", exception);
        }
    }

    private static Color randomColor(int min, int max) {
        return new Color(min + RANDOM.nextInt(max - min), min + RANDOM.nextInt(max - min), min + RANDOM.nextInt(max - min));
    }

    /**
     * 验证码存储接口
     */
    public interface CaptchaStore {
        /**
         * 保存验证码及其过期时间
         */
        void put(String id, String code, Duration ttl);

        /**
         * 读取并删除验证码，保证验证码只使用一次
         */
        String consume(String id);
    }

    /**
     * 验证码响应体
     */
    public record CaptchaResponse(boolean enabled, String captchaId, String image) { }
}
