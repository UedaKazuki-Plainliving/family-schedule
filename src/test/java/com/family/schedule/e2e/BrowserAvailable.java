package com.family.schedule.e2e;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** 実環境に Playwright ブラウザが入っているかどうかを判定する */
public final class BrowserAvailable {
    public static boolean check() {
        String env = System.getenv("PLAYWRIGHT_BROWSERS_PATH");
        if (env != null && hasChromium(Paths.get(env))) return true;
        String home = System.getProperty("user.home");
        if (hasChromium(Paths.get(home, ".cache", "ms-playwright"))) return true;
        return hasChromium(Paths.get("/opt/pw-browsers"));
    }

    private static boolean hasChromium(Path dir) {
        if (!Files.isDirectory(dir)) return false;
        try (var stream = Files.list(dir)) {
            return stream.anyMatch(p -> p.getFileName().toString().startsWith("chromium-")
                    || p.getFileName().toString().startsWith("firefox-")
                    || p.getFileName().toString().startsWith("webkit-"));
        } catch (Exception e) {
            return false;
        }
    }
    private BrowserAvailable() {}
}
