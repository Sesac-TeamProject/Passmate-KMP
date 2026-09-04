import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 패스메이트 브랜드 에셋 생성기.
 *
 * 시안(Passmate-Frontend/design/design.pen)의 `11 · 브랜드 — 로고` 프레임에서 뽑은 심볼 path
 * 하나를 원본으로 삼아 3플랫폼 런처 아이콘과 스플래시 이미지를 만든다.
 *
 *   java tools/brand/BrandAssets.java
 *
 * JDK 11+ 단일 파일 실행이라 별도 빌드가 없다. 외부 도구(rsvg·ImageMagick)도 쓰지 않는다.
 */
public class BrandAssets {

    static final Color MINT = new Color(0x17B884);
    static final Color INK = new Color(0x1B1F24);
    static final Color WHITE = Color.WHITE;

    static final String MINT_HEX = "#17B884";
    static final String WHITE_HEX = "#FFFFFF";

    // --- 시안에서 추출한 심볼 지오메트리 (logo/symbol, 81.25 x 104) ---
    static final double SYMBOL_W = 81.25;
    static final double SYMBOL_H = 104.0;

    // 바깥 P — viewBox 37.5 x 48
    static final String P_PATH =
        "M3 0l17 0c4.64129 0 9.09248 1.84374 12.37437 5.12563 "
        + "3.28189 3.28189 5.12563 7.73308 5.12563 12.37437 "
        + "0 4.64129-1.84375 9.09248-5.12563 12.37437-3.28189 3.28189-7.73308 5.12563-12.37437 5.12563"
        + "l-7 0 0 10c0 0.79565-0.31607 1.55871-0.87868 2.12132-0.56261 0.56261-1.32567 0.87868-2.12132 0.87868"
        + "l-7 0c-0.79565 0-1.55871-0.31607-2.12132-0.87868-0.56261-0.56261-0.87868-1.32567-0.87868-2.12132"
        + "l0-42c0-0.79565 0.31607-1.55871 0.87868-2.12132 0.56261-0.56261 1.32567-0.87868 2.12132-0.87868z";

    // 안쪽으로 파인 M — viewBox 13.77 x 13.6. 배경색으로 칠해 덩어리를 파낸다
    static final String N_PATH = "M0 0l13.77 0-9.18 6.8 9.18 6.8-13.77 0 0-13.6z";

    static final double P_VIEWBOX_W = 37.5;
    static final double N_OFFSET_X = 28.426668;
    static final double N_OFFSET_Y = 24.266666;
    static final double N_STROKE_NODE = 5.4166670;

    static final double INNER_SCALE = SYMBOL_W / P_VIEWBOX_W;      // 2.1666667
    static final double N_STROKE = N_STROKE_NODE / INNER_SCALE;    // 2.5 (스케일 전 좌표계)

    // 시안 규칙 — 아이콘 반경은 한 변의 22.4%, 심볼 높이는 한 변의 62%
    static final double CORNER_RATIO = 0.224;
    static final double SYMBOL_RATIO = 0.62;

    enum Shape { SQUIRCLE, CIRCLE, SQUARE, NONE }

    static Path2D.Double parsePath(String d) {
        Path2D.Double path = new Path2D.Double();
        double cx = 0, cy = 0;
        int i = 0;
        char command = 0;

        while (i < d.length()) {
            char c = d.charAt(i);
            if (c == ' ' || c == ',') {
                i++;
                continue;
            }
            if (Character.isLetter(c)) {
                command = c;
                i++;
                if (command == 'z' || command == 'Z') {
                    path.closePath();
                }
                continue;
            }

            double[] n = new double[6];
            int need = (command == 'c' || command == 'C') ? 6 : 2;
            int[] cursor = {i};
            for (int k = 0; k < need; k++) {
                n[k] = readNumber(d, cursor);
            }
            i = cursor[0];

            switch (command) {
                case 'M' -> { cx = n[0]; cy = n[1]; path.moveTo(cx, cy); command = 'L'; }
                case 'L' -> { cx = n[0]; cy = n[1]; path.lineTo(cx, cy); }
                case 'l' -> { cx += n[0]; cy += n[1]; path.lineTo(cx, cy); }
                case 'c' -> {
                    path.curveTo(cx + n[0], cy + n[1], cx + n[2], cy + n[3], cx + n[4], cy + n[5]);
                    cx += n[4];
                    cy += n[5];
                }
                case 'C' -> { path.curveTo(n[0], n[1], n[2], n[3], n[4], n[5]); cx = n[4]; cy = n[5]; }
                default -> throw new IllegalStateException("지원하지 않는 path 명령: " + command);
            }
        }
        return path;
    }

    static double readNumber(String d, int[] cursor) {
        int i = cursor[0];
        while (i < d.length() && (d.charAt(i) == ' ' || d.charAt(i) == ',')) {
            i++;
        }
        int start = i;
        if (i < d.length() && (d.charAt(i) == '-' || d.charAt(i) == '+')) {
            i++;
        }
        while (i < d.length() && (Character.isDigit(d.charAt(i)) || d.charAt(i) == '.')) {
            i++;
        }
        cursor[0] = i;
        return Double.parseDouble(d.substring(start, i));
    }

    /** 심볼 두 겹(바깥 P + 배경색으로 파낸 자리)을 그린다. */
    static void drawSymbol(Graphics2D g, double scale, double tx, double ty, Color fg, Color bg) {
        AffineTransform base = new AffineTransform();
        base.translate(tx, ty);
        base.scale(scale, scale);

        AffineTransform outer = new AffineTransform(base);
        outer.scale(INNER_SCALE, INNER_SCALE);
        g.setColor(fg);
        g.fill(outer.createTransformedShape(parsePath(P_PATH)));

        Path2D.Double notch = parsePath(N_PATH);
        Area carved = new Area(notch);
        carved.add(new Area(new BasicStroke((float) N_STROKE, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)
            .createStrokedShape(notch)));

        AffineTransform inner = new AffineTransform(base);
        inner.translate(N_OFFSET_X, N_OFFSET_Y);
        inner.scale(INNER_SCALE, INNER_SCALE);
        g.setColor(bg);
        g.fill(inner.createTransformedShape(carved));
    }

    static Graphics2D canvas(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    /** 앱 아이콘 한 장. 배경 밖은 투명하게 남긴다. */
    static BufferedImage icon(int side, Color bg, Color fg, Shape shape) {
        BufferedImage img = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas(img);

        g.setColor(bg);
        switch (shape) {
            case SQUIRCLE -> {
                double r = side * CORNER_RATIO * 2;
                g.fill(new RoundRectangle2D.Double(0, 0, side, side, r, r));
            }
            case CIRCLE -> g.fill(new Ellipse2D.Double(0, 0, side, side));
            case SQUARE -> g.fill(new Rectangle2D.Double(0, 0, side, side));
            case NONE -> { }
        }

        double height = side * SYMBOL_RATIO;
        double scale = height / SYMBOL_H;
        double width = SYMBOL_W * scale;
        drawSymbol(g, scale, (side - width) / 2.0, (side - height) / 2.0, fg, bg);

        g.dispose();
        return img;
    }

    /** 배경 없는 심볼 단독 — iOS 런치 스크린처럼 배경색이 이미 깔린 자리에 얹는다. */
    static BufferedImage symbolOnly(int height, Color fg, Color bg) {
        int width = (int) Math.round(SYMBOL_W / SYMBOL_H * height);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas(img);

        drawSymbol(g, height / SYMBOL_H, 0, 0, fg, bg);

        g.dispose();
        return img;
    }

    /** 안드로이드 벡터 드로어블 — SVG와 같은 변환을 group 두 개로 평탄화해 적는다. */
    static String vectorDrawable(int sizeDp, int viewport, double symbolHeight, String fg, String bg, String note) {
        double scale = symbolHeight / SYMBOL_H;
        double width = SYMBOL_W * scale;
        double tx = (viewport - width) / 2.0;
        double ty = (viewport - symbolHeight) / 2.0;
        double inner = scale * INNER_SCALE;

        return String.format("""
            <?xml version="1.0" encoding="utf-8"?>
            <!-- 자동 생성 — tools/brand/BrandAssets.java (%s). 직접 고치지 말 것 -->
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="%ddp"
                android:height="%ddp"
                android:viewportWidth="%d"
                android:viewportHeight="%d">
                <group
                    android:translateX="%.4f"
                    android:translateY="%.4f"
                    android:scaleX="%.7f"
                    android:scaleY="%.7f">
                    <path
                        android:fillColor="%s"
                        android:pathData="%s" />
                </group>
                <group
                    android:translateX="%.4f"
                    android:translateY="%.4f"
                    android:scaleX="%.7f"
                    android:scaleY="%.7f">
                    <path
                        android:fillColor="%s"
                        android:strokeColor="%s"
                        android:strokeWidth="%s"
                        android:strokeLineJoin="round"
                        android:pathData="%s" />
                </group>
            </vector>
            """, note, sizeDp, sizeDp, viewport, viewport,
            tx, ty, inner, inner, fg, P_PATH,
            tx + N_OFFSET_X * scale, ty + N_OFFSET_Y * scale, inner, inner,
            bg, bg, N_STROKE, N_PATH);
    }

    /** 둥근 사각형 마크 — 시안 '심볼 단독 · 민트 배경 위'. 반경 = 한 변의 22.4%, 심볼 높이 = 62%. */
    static String roundedSquarePath(double side) {
        double r = side * CORNER_RATIO;

        return String.format(
            "M%.4f,0 H%.4f A%.4f,%.4f 0 0 1 %.4f,%.4f V%.4f A%.4f,%.4f 0 0 1 %.4f,%.4f H%.4f "
                + "A%.4f,%.4f 0 0 1 0,%.4f V%.4f A%.4f,%.4f 0 0 1 %.4f,0 Z",
            r, side - r, r, r, side, r, side - r, r, r, side - r, side, r,
            r, r, side - r, r, r, r, r);
    }

    /** 마크의 안드로이드 벡터 드로어블 — 배경 도형 + 심볼 두 그룹. */
    static String markVector(int side, String bgHex, String fgHex, String note) {
        double symbolHeight = side * SYMBOL_RATIO;
        double scale = symbolHeight / SYMBOL_H;
        double width = SYMBOL_W * scale;
        double tx = (side - width) / 2.0;
        double ty = (side - symbolHeight) / 2.0;
        double inner = scale * INNER_SCALE;

        return String.format("""
            <?xml version="1.0" encoding="utf-8"?>
            <!-- 자동 생성 — tools/brand/BrandAssets.java (%s). 직접 고치지 말 것 -->
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="%ddp"
                android:height="%ddp"
                android:viewportWidth="%d"
                android:viewportHeight="%d">
                <path
                    android:fillColor="%s"
                    android:pathData="%s" />
                <group
                    android:translateX="%.4f"
                    android:translateY="%.4f"
                    android:scaleX="%.7f"
                    android:scaleY="%.7f">
                    <path
                        android:fillColor="%s"
                        android:pathData="%s" />
                </group>
                <group
                    android:translateX="%.4f"
                    android:translateY="%.4f"
                    android:scaleX="%.7f"
                    android:scaleY="%.7f">
                    <path
                        android:fillColor="%s"
                        android:strokeColor="%s"
                        android:strokeWidth="%s"
                        android:strokeLineJoin="round"
                        android:pathData="%s" />
                </group>
            </vector>
            """, note, side, side, side, side,
            bgHex, roundedSquarePath(side),
            tx, ty, inner, inner, fgHex, P_PATH,
            tx + N_OFFSET_X * scale, ty + N_OFFSET_Y * scale, inner, inner,
            bgHex, bgHex, N_STROKE, N_PATH);
    }

    /** 위 벡터의 iOS 사본 — 같은 변환을 SVG로 적는다(벡터 보존 에셋). */
    static String markSvg(int side, String bgHex, String fgHex) {
        double symbolHeight = side * SYMBOL_RATIO;
        double scale = symbolHeight / SYMBOL_H;
        double width = SYMBOL_W * scale;
        double tx = (side - width) / 2.0;
        double ty = (side - symbolHeight) / 2.0;
        double inner = scale * INNER_SCALE;

        return String.format("""
            <!-- 자동 생성 — tools/brand/BrandAssets.java (brand mark). 직접 고치지 말 것 -->
            <svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d">
              <path fill="%s" d="%s"/>
              <g transform="translate(%.4f,%.4f) scale(%.7f)">
                <path fill="%s" d="%s"/>
              </g>
              <g transform="translate(%.4f,%.4f) scale(%.7f)">
                <path fill="%s" stroke="%s" stroke-width="%s" stroke-linejoin="round" d="%s"/>
              </g>
            </svg>
            """, side, side, side, side,
            bgHex, roundedSquarePath(side),
            tx, ty, inner, fgHex, P_PATH,
            tx + N_OFFSET_X * scale, ty + N_OFFSET_Y * scale, inner,
            bgHex, bgHex, N_STROKE, N_PATH);
    }

    static final String MARK_IMAGESET_CONTENTS = """
        {
          "images" : [
            {
              "filename" : "brand-mark.svg",
              "idiom" : "universal"
            }
          ],
          "info" : {
            "author" : "xcode",
            "version" : 1
          },
          "properties" : {
            "preserves-vector-representation" : true
          }
        }
        """;

    static final String SOLID_BACKGROUND = """
        <?xml version="1.0" encoding="utf-8"?>
        <!-- 자동 생성 — tools/brand/BrandAssets.java. 시안 앱 아이콘 배경(민트 단색) -->
        <vector xmlns:android="http://schemas.android.com/apk/res/android"
            android:width="108dp"
            android:height="108dp"
            android:viewportWidth="108"
            android:viewportHeight="108">
            <path
                android:fillColor="#17B884"
                android:pathData="M0,0h108v108h-108z" />
        </vector>
        """;

    static Path repo;
    static List<String> written = new ArrayList<>();

    static void writeText(String relative, String body) throws IOException {
        Path out = repo.resolve(relative);
        Files.createDirectories(out.getParent());
        Files.writeString(out, body, StandardCharsets.UTF_8);
        written.add(relative);
    }

    static void writePng(String relative, BufferedImage img) throws IOException {
        Path out = repo.resolve(relative);
        Files.createDirectories(out.getParent());
        ImageIO.write(img, "png", out.toFile());
        written.add(relative);
    }

    public static void main(String[] args) throws IOException {
        repo = Paths.get("").toAbsolutePath();
        if (!Files.isDirectory(repo.resolve("composeApp")) || !Files.isDirectory(repo.resolve("iosApp"))) {
            System.err.println("리포 루트에서 실행해 주세요: java tools/brand/BrandAssets.java");
            System.exit(1);
        }

        String androidRes = "composeApp/src/androidMain/res/";

        // 1) 어댑티브 아이콘 — 108dp 캔버스에서 보이는 영역은 72dp, 그 62%가 심볼 높이
        writeText(androidRes + "drawable/ic_launcher_background.xml", SOLID_BACKGROUND);
        writeText(androidRes + "drawable/ic_launcher_foreground.xml",
            vectorDrawable(108, 108, 72 * SYMBOL_RATIO, WHITE_HEX, MINT_HEX, "adaptive foreground"));

        // 2) 안드로이드 12+ 스플래시 아이콘 — 240dp 중 가운데 160dp 안에 (시안 '아이콘 세이프')
        writeText(androidRes + "drawable/ic_splash_logo.xml",
            vectorDrawable(240, 240, 160 * SYMBOL_RATIO, WHITE_HEX, MINT_HEX, "splash icon"));

        // 3) 레거시 런처 아이콘 — API 24·25는 어댑티브를 모르고 마스킹도 안 하므로 모양을 직접 굽는다
        int[][] densities = {{48, 0}, {72, 1}, {96, 2}, {144, 3}, {192, 4}};
        String[] names = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        for (int[] d : densities) {
            int px = d[0];
            String dir = androidRes + "mipmap-" + names[d[1]] + "/";
            writePng(dir + "ic_launcher.png", icon(px, MINT, WHITE, Shape.SQUIRCLE));
            writePng(dir + "ic_launcher_round.png", icon(px, MINT, WHITE, Shape.CIRCLE));
        }

        // 4) iOS 앱 아이콘 — 마스크는 시스템이 씌우므로 정사각 풀블리드로 낸다
        String appIcon = "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/";
        writePng(appIcon + "AppIcon-1024.png", icon(1024, MINT, WHITE, Shape.SQUARE));
        writePng(appIcon + "AppIcon-1024-dark.png", icon(1024, INK, MINT, Shape.SQUARE));
        writePng(appIcon + "AppIcon-1024-tinted.png", icon(1024, INK, WHITE, Shape.SQUARE));

        // 5) iOS 런치 스크린 로고 — 민트 배경 위에 얹히므로 파인 자리는 민트로 채운다
        String launch = "iosApp/iosApp/Assets.xcassets/LaunchLogo.imageset/";
        int[] launchScales = {104, 208, 312};   // 시안 M-00의 심볼 높이 104pt × @1x·@2x·@3x
        for (int i = 0; i < launchScales.length; i++) {
            writePng(launch + "LaunchLogo@" + (i + 1) + "x.png", symbolOnly(launchScales[i], WHITE, MINT));
        }

        // 7) 브랜드 마크 — 로고 락업(로그인 화면 등)에 쓰는 둥근 사각형 형태.
        //    시안 '11 · 브랜드 — 로고' > 심볼 단독 · 민트 배경 위 = 민트 바탕에 흰 심볼
        String mark = markVector(100, MINT_HEX, WHITE_HEX, "brand mark (mint square)");
        writeText(androidRes + "drawable/ic_brand_mark.xml", mark);
        writeText("composeApp/src/jvmMain/resources/drawable/ic_brand_mark.xml", mark);

        String brandMark = "iosApp/iosApp/Assets.xcassets/BrandMark.imageset/";
        writeText(brandMark + "brand-mark.svg", markSvg(100, MINT_HEX, WHITE_HEX));
        writeText(brandMark + "Contents.json", MARK_IMAGESET_CONTENTS);

        // 6) 데스크톱 윈도우 아이콘
        writePng("composeApp/src/jvmMain/resources/passmate-icon.png", icon(512, MINT, WHITE, Shape.SQUIRCLE));

        System.out.println("생성 " + written.size() + "개:");
        written.forEach(p -> System.out.println("  " + p));
    }
}
