package io.github.onedream921.alphavue.modules.auth.service;

import io.github.onedream921.alphavue.modules.auth.config.CaptchaProperties;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.Path2D;
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
    private static final int SLIDER_WIDTH = 420;
    private static final int SLIDER_HEIGHT = 280;
    private static final int SLIDER_PIECE_WIDTH = 42;
    private static final int SLIDER_MAX_OFFSET = SLIDER_WIDTH - SLIDER_PIECE_WIDTH;
    private static final String[] SLIDER_SCENES = {
            "captcha/scene-books.jpg",
            "captcha/scene-city.jpg",
            "captcha/scene-courtyard.jpg"
    };
    private final CaptchaProperties properties;
    private final CaptchaStore store;
    private final SystemSettingService settingService;

    public CaptchaService(CaptchaProperties properties, CaptchaStore store) {
        this(properties, store, null);
    }

    @Autowired
    public CaptchaService(CaptchaProperties properties, CaptchaStore store, SystemSettingService settingService) {
        this.properties = properties;
        this.store = store;
        this.settingService = settingService;
    }

    /**
     * 创建验证码挑战；验证码关闭时返回禁用状态
     */
    public CaptchaResponse create() {
        String type = captchaType();
        boolean rememberMeEnabled = rememberMeEnabled();
        if (!captchaEnabled()) {
            return new CaptchaResponse(false, type, rememberMeEnabled, null, null, null,
                    null, null, null, null, null, null);
        }
        String id = UUID.randomUUID().toString();
        if ("slider".equals(type)) {
            int targetX = 240 + RANDOM.nextInt(121);
            SliderVisual visual = renderSlider(targetX);
            store.put(id, "slider:" + targetX, properties.getCaptchaTtl());
            return new CaptchaResponse(true, type, rememberMeEnabled, id, null, null,
                    visual.background(), visual.piece(), visual.width(), visual.height(),
                    visual.pieceWidth(), visual.pieceTop());
        }
        String code = createNumericCode();
        store.put(id, code, properties.getCaptchaTtl());
        return new CaptchaResponse(true, type, rememberMeEnabled, id, render(code),
                null,
                null, null, null, null, null, null);
    }

    /**
     * 校验提交的验证码；滑块挑战同时检查位置、耗时和轨迹，且挑战只允许消费一次。
     */
    public void validate(String id, String submittedCode) {
        if (!captchaEnabled()) {
            return;
        }
        String expected = id == null ? null : store.consume(id);
        boolean valid = false;
        if (expected != null && submittedCode != null) {
            String submitted = submittedCode.trim();
            if (expected.startsWith("slider:")) {
                valid = validateSlider(expected.substring("slider:".length()), submitted);
            } else {
                valid = expected.equalsIgnoreCase(submitted);
            }
        }
        if (!valid) {
            throw new BusinessException(401, PublicErrorMessage.INVALID_CREDENTIALS);
        }
    }

    private static SliderVisual renderSlider(int targetX) {
        int width = SLIDER_WIDTH;
        int height = SLIDER_HEIGHT;
        int pieceWidth = SLIDER_PIECE_WIDTH;
        int pieceTop = 56 + RANDOM.nextInt(height - pieceWidth - 96);
        BufferedImage source;
        try {
            String scene = SLIDER_SCENES[RANDOM.nextInt(SLIDER_SCENES.length)];
            source = ImageIO.read(new ClassPathResource(scene).getInputStream());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load captcha scene", exception);
        }
        Graphics2D graphics = source.createGraphics();
        try {
            graphics.setColor(new Color(8, 20, 34, 38));
            graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        } finally {
            graphics.dispose();
        }
        BufferedImage originalDisplayed = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D originalGraphics = originalDisplayed.createGraphics();
        try {
            originalGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            originalGraphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            originalGraphics.dispose();
        }
        BufferedImage displayed = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D displayedGraphics = displayed.createGraphics();
        try {
            displayedGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            displayedGraphics.drawImage(originalDisplayed, 0, 0, null);
            displayedGraphics.setColor(new Color(248, 250, 252, 180));
            displayedGraphics.fill(puzzleShape(targetX, pieceTop, pieceWidth));
            displayedGraphics.setColor(new Color(255, 255, 255, 90));
            displayedGraphics.setStroke(new BasicStroke(1.2f));
            displayedGraphics.draw(puzzleShape(targetX, pieceTop, pieceWidth));
        } finally {
            displayedGraphics.dispose();
        }
        BufferedImage piece = new BufferedImage(pieceWidth, pieceWidth, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pieceGraphics = piece.createGraphics();
        try {
            pieceGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pieceGraphics.setClip(puzzleShape(0, 0, pieceWidth));
            pieceGraphics.drawImage(originalDisplayed, -targetX, -pieceTop, null);
            pieceGraphics.setClip(null);
            pieceGraphics.setColor(new Color(255, 255, 255, 120));
            pieceGraphics.setStroke(new BasicStroke(1.2f));
            pieceGraphics.draw(puzzleShape(0, 0, pieceWidth));
        } finally {
            pieceGraphics.dispose();
        }
        return new SliderVisual(dataUrl(displayed, "jpg"), dataUrl(piece, "png"), width, height,
                pieceWidth, pieceTop);
    }

    private static Path2D.Double puzzleShape(double x, double y, double size) {
        double tab = size * 0.24;
        double shoulder = size * 0.27;
        double middle = size * 0.5;
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x, y);
        path.lineTo(x + shoulder, y);
        path.curveTo(x + shoulder + tab, y, x + middle - tab, y + tab, x + middle, y + tab);
        path.curveTo(x + middle + tab, y + tab, x + size - shoulder - tab, y, x + size - shoulder, y);
        path.lineTo(x + size, y);
        path.lineTo(x + size, y + shoulder);
        path.curveTo(x + size, y + shoulder + tab, x + size - tab, y + middle - tab, x + size - tab, y + middle);
        path.curveTo(x + size - tab, y + middle + tab, x + size, y + size - shoulder - tab, x + size, y + size - shoulder);
        path.lineTo(x + size, y + size);
        path.lineTo(x + size - shoulder, y + size);
        path.curveTo(x + size - shoulder - tab, y + size, x + middle + tab, y + size - tab, x + middle, y + size - tab);
        path.curveTo(x + middle - tab, y + size - tab, x + shoulder + tab, y + size, x + shoulder, y + size);
        path.lineTo(x, y + size);
        path.lineTo(x, y + size - shoulder);
        path.curveTo(x, y + size - shoulder - tab, x + tab, y + middle + tab, x + tab, y + middle);
        path.curveTo(x + tab, y + middle - tab, x, y + shoulder + tab, x, y + shoulder);
        path.closePath();
        return path;
    }

    private static boolean validateSlider(String targetText, String submitted) {
        try {
            String[] parts = submitted.split("~", 3);
            if (parts.length != 3) return false;
            int target = Integer.parseInt(targetText);
            int submittedX = Integer.parseInt(parts[0]);
            long duration = Long.parseLong(parts[1]);
            String[] points = parts[2].split(";");
            if (target < 0 || target > SLIDER_MAX_OFFSET || submittedX < 0 || submittedX > SLIDER_MAX_OFFSET
                    || points.length < 4 || duration < 350 || duration > 30_000) return false;
            int previousX = Integer.MIN_VALUE;
            long previousTime = -1;
            int lastX = Integer.MIN_VALUE;
            long lastTime = -1;
            for (String point : points) {
                String[] values = point.split(",");
                if (values.length != 3) return false;
                int x = Integer.parseInt(values[0]);
                int y = Integer.parseInt(values[1]);
                long time = Long.parseLong(values[2]);
                if (x < 0 || x > SLIDER_WIDTH || y < 0 || y > 48 || time < 0 || time > duration
                        || (previousTime >= 0 && (time <= previousTime || Math.abs(x - previousX) > 90))) {
                    return false;
                }
                previousX = x;
                previousTime = time;
                lastX = x;
                lastTime = time;
            }
            return lastTime >= 0 && lastTime <= duration && Math.abs(lastX - submittedX) <= 8
                    && Math.abs(target - submittedX) <= 8;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static String dataUrl(BufferedImage image, String format) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, output);
            return "data:image/" + format + ";base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to render captcha", exception);
        }
    }

    private record SliderVisual(String background, String piece, int width, int height,
                                int pieceWidth, int pieceTop) { }

    private static String createNumericCode() {
        StringBuilder code = new StringBuilder(6);
        for (int index = 0; index < 6; index++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }

    private static String render(String code) {
        BufferedImage image = new BufferedImage(160, 60, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(242 + RANDOM.nextInt(10), 245 + RANDOM.nextInt(8), 250));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setStroke(new BasicStroke(1.2f));
            for (int index = 0; index < 11; index++) {
                graphics.setColor(randomColor(120, 210));
                graphics.drawLine(RANDOM.nextInt(160), RANDOM.nextInt(60), RANDOM.nextInt(160), RANDOM.nextInt(60));
            }
            for (int index = 0; index < 220; index++) {
                graphics.setColor(randomColor(160, 230));
                graphics.fillRect(RANDOM.nextInt(160), RANDOM.nextInt(60), 1 + RANDOM.nextInt(2), 1 + RANDOM.nextInt(2));
            }
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
            for (int index = 0; index < code.length(); index++) {
                Graphics2D character = (Graphics2D) graphics.create();
                try {
                    character.setColor(randomColor(20, 100));
                    character.rotate(Math.toRadians(RANDOM.nextInt(25) - 12), 18 + index * 25, 34);
                    character.drawString(String.valueOf(code.charAt(index)), 7 + index * 25 + RANDOM.nextInt(3),
                            42 + RANDOM.nextInt(5) - 2);
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

    private boolean captchaEnabled() {
        if (settingService == null) {
            return properties.isCaptchaEnabled();
        }
        Object configured = settingService.get(SettingGroup.LOGIN).values().get("captchaEnabled");
        return configured instanceof Boolean value ? value
                : configured instanceof String value ? Boolean.parseBoolean(value)
                : properties.isCaptchaEnabled();
    }

    private String captchaType() {
        if (settingService == null) return "numeric";
        Object configured = settingService.get(SettingGroup.LOGIN).values().get("captchaType");
        return configured != null && "slider".equalsIgnoreCase(String.valueOf(configured)) ? "slider" : "numeric";
    }

    private boolean rememberMeEnabled() {
        if (settingService == null) return true;
        Object configured = settingService.get(SettingGroup.LOGIN).values().get("rememberMeEnabled");
        return configured instanceof Boolean value ? value
                : configured instanceof String value ? Boolean.parseBoolean(value) : true;
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
    public record CaptchaResponse(boolean enabled, String type, boolean rememberMeEnabled,
                                 String captchaId, String image, String question,
                                 String sliderBackground, String sliderPiece, Integer sliderWidth,
                                 Integer sliderHeight, Integer sliderPieceWidth, Integer sliderPieceTop) { }
}
