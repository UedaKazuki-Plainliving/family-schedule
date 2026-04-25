package com.family.schedule.bdd;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Stream;

public class BddHooks {

    private static volatile boolean initialized = false;
    static Browser browser;
    private static Playwright playwright;

    @Autowired WorldContext world;
    @Autowired JdbcTemplate jdbc;
    @LocalServerPort int port;

    @Before
    public void setUp() {
        if (!initialized) {
            synchronized (BddHooks.class) {
                if (!initialized) {
                    Path chrome = findChrome();
                    if (chrome != null) {
                        playwright = Playwright.create(new Playwright.CreateOptions()
                                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
                        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                                .setHeadless(true)
                                .setExecutablePath(chrome));
                    }
                    initialized = true;
                }
            }
        }

        jdbc.execute("DELETE FROM schedules");
        jdbc.execute("ALTER TABLE schedules ALTER COLUMN id RESTART WITH 1");

        world.port = port;
        world.featureToday = LocalDate.now();
        world.viewDate = LocalDate.now();
        world.stepCount = 0;

        if (browser != null) {
            var ctx = browser.newContext(new Browser.NewContextOptions().setViewportSize(390, 844));
            world.page = ctx.newPage();
            world.page.setDefaultTimeout(5000);
        }
    }

    @After
    public void tearDown() {
        if (world.page != null) {
            try { world.page.context().close(); } catch (Exception ignored) {}
            world.page = null;
        }
    }

    private static Path findChrome() {
        String envPath = System.getenv("PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH");
        if (envPath != null && Files.isExecutable(Paths.get(envPath))) return Paths.get(envPath);

        String[] roots = {
                System.getenv("PLAYWRIGHT_BROWSERS_PATH"),
                "/opt/pw-browsers",
                System.getProperty("user.home") + "/.cache/ms-playwright",
                System.getProperty("user.home") + "/AppData/Local/ms-playwright"
        };
        for (String root : roots) {
            if (root == null) continue;
            Path rootPath = Paths.get(root);
            if (!Files.isDirectory(rootPath)) continue;
            try (Stream<Path> s = Files.list(rootPath)) {
                var hit = s.filter(x -> x.getFileName().toString().startsWith("chromium-"))
                        .sorted(java.util.Comparator.reverseOrder())
                        .flatMap(dir -> { try { return Files.list(dir); } catch (Exception e) { return Stream.empty(); } })
                        .filter(Files::isDirectory)
                        .flatMap(dir -> { try { return Files.list(dir); } catch (Exception e) { return Stream.empty(); } })
                        .filter(x -> {
                            String n = x.getFileName().toString();
                            return (n.equals("chrome") || n.equals("chrome.exe")) && Files.isRegularFile(x);
                        })
                        .findFirst();
                if (hit.isPresent()) return hit.get();
            } catch (Exception ignored) {}
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String sep = System.getProperty("path.separator", ":");
            for (String cmd : new String[]{"chromium", "chromium-browser", "google-chrome", "chrome"}) {
                for (String dir : pathEnv.split(sep)) {
                    Path candidate = Paths.get(dir, cmd);
                    if (Files.isExecutable(candidate)) return candidate;
                }
            }
        }
        return null;
    }
}
