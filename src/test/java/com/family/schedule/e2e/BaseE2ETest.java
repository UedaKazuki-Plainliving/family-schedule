package com.family.schedule.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseE2ETest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbc;

    protected static Playwright playwright;
    protected static Browser browser;

    protected BrowserContext context;
    protected Page page;
    protected String currentTestName;

    protected static final Path EVIDENCE_DIR =
            Paths.get(System.getProperty("user.dir"), "target", "evidence");
    protected static final Path VIDEO_DIR =
            Paths.get(System.getProperty("user.dir"), "target", "evidence", "videos");

    @BeforeAll
    static void init() {
        Path chrome = findChromeExecutable();
        Assumptions.assumeTrue(chrome != null,
                "Chromium バイナリが見つかりません：E2Eテストをスキップします");
        // Playwright のブラウザ自動ダウンロードをスキップ
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setExecutablePath(chrome));
    }

    private static Path findChromeExecutable() {
        // 1) 環境変数 PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH があれば最優先
        String p = System.getenv("PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH");
        if (p != null && Files.isExecutable(Paths.get(p))) return Paths.get(p);

        // 2) 既知の配置を探索
        for (String root : new String[]{
                System.getenv("PLAYWRIGHT_BROWSERS_PATH"),
                "/opt/pw-browsers",
                System.getProperty("user.home") + "/.cache/ms-playwright"
        }) {
            if (root == null) continue;
            Path rootPath = Paths.get(root);
            if (!Files.isDirectory(rootPath)) continue;
            try (Stream<Path> s = Files.list(rootPath)) {
                var hit = s.filter(x -> x.getFileName().toString().startsWith("chromium-"))
                        .sorted(java.util.Comparator.reverseOrder())
                        .flatMap(dir -> {
                            try { return Files.list(dir); } catch (Exception e) { return Stream.empty(); }
                        })
                        .filter(x -> Files.isDirectory(x))
                        .map(x -> x.resolve("chrome"))
                        .filter(Files::isExecutable)
                        .findFirst();
                if (hit.isPresent()) return hit.get();
            } catch (Exception ignored) {}
        }
        // 3) PATH 上の chromium / chrome
        for (String cmd : new String[]{"chromium", "chromium-browser", "google-chrome", "chrome"}) {
            for (String dir : System.getenv("PATH").split(":")) {
                Path candidate = Paths.get(dir, cmd);
                if (Files.isExecutable(candidate)) return candidate;
            }
        }
        return null;
    }

    @AfterAll
    static void close() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void setUp(TestInfo info) {
        jdbc.execute("DELETE FROM schedules");
        jdbc.execute("ALTER TABLE schedules ALTER COLUMN id RESTART WITH 1");
        try {
            Files.createDirectories(EVIDENCE_DIR);
            Files.createDirectories(VIDEO_DIR);
        } catch (Exception ignored) {}
        currentTestName = info.getTestClass()
                .map(Class::getSimpleName).orElse("test")
                + "_" + info.getDisplayName().replaceAll("[^A-Za-z0-9_一-龯ぁ-んァ-ヶー]", "_");
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(390, 844) // iPhone 13 size
                .setRecordVideoDir(VIDEO_DIR)
                .setRecordVideoSize(390, 844));
        page = context.newPage();
        page.onDialog(dialog -> dialog.accept());
    }

    @AfterEach
    void tearDown() {
        // 各テスト終了時に最終状態のスクリーンショットを撮る（証跡）
        try {
            if (page != null && !page.isClosed()) {
                snap("end");
            }
        } catch (Exception ignored) {}
        // 動画を ASCII 安全なファイル名にリネームして保存
        try {
            if (page != null) {
                var video = page.video();
                if (context != null) context.close();  // close() で録画ファイルが確定する
                if (video != null) {
                    String safe = currentTestName.replaceAll("[^A-Za-z0-9_]", "_");
                    Path dest = VIDEO_DIR.resolve(safe + ".webm");
                    video.saveAs(dest);
                    try { video.delete(); } catch (Exception ignored) {}
                    Files.writeString(VIDEO_DIR.resolve("_map.tsv"),
                            dest.getFileName() + "\t" + currentTestName + "\n",
                            java.nio.charset.StandardCharsets.UTF_8,
                            java.nio.file.StandardOpenOption.CREATE,
                            java.nio.file.StandardOpenOption.APPEND);
                    return;
                }
            }
        } catch (Exception ignored) {}
        if (context != null) context.close();
    }

    /** 指定タグでスクリーンショットを target/evidence 配下に保存する。
     *  ファイル名はASCII安全（OSロケール非依存）。マッピング表をJSONで残す。 */
    protected void snap(String tag) {
        String safeName = currentTestName.replaceAll("[^A-Za-z0-9_]", "_");
        String safeTag = tag.replaceAll("[^A-Za-z0-9_]", "_");
        Path file = EVIDENCE_DIR.resolve(safeName + "_" + safeTag + ".png");
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(file)
                .setFullPage(true));
        // 元の日本語名も後で参照できるようマッピングを書き出す
        Path map = EVIDENCE_DIR.resolve("_map.tsv");
        try {
            Files.writeString(map,
                    file.getFileName() + "\t" + currentTestName + " / " + tag + "\n",
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected void setCurrentUser(String name, int id) {
        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser'," +
                "JSON.stringify({id:" + id + ",name:\"" + name + "\",displayOrder:" + id + "}));");
    }

    protected Locator cell(int memberId, String date) {
        return page.locator(".schedule-cell[data-member-id='" + memberId + "'][data-date='" + date + "']");
    }

    protected void insertSchedule(int memberId, String date, String content) {
        jdbc.update(
                "INSERT INTO schedules (member_id, date, content, created_at, updated_at) VALUES (?,?,?,now(),now())",
                memberId, java.sql.Date.valueOf(date), content);
    }

    protected String today() { return LocalDate.now().toString(); }
    protected String tomorrow() { return LocalDate.now().plusDays(1).toString(); }
    protected String dayAfterTomorrow() { return LocalDate.now().plusDays(2).toString(); }
    protected String daysLater(int n) { return LocalDate.now().plusDays(n).toString(); }

    protected void flickLeft() {
        page.mouse().move(300, 250); page.mouse().down();
        page.mouse().move(100, 250); page.mouse().up();
        page.waitForTimeout(500);
    }

    protected void flickRight() {
        page.mouse().move(100, 250); page.mouse().down();
        page.mouse().move(300, 250); page.mouse().up();
        page.waitForTimeout(500);
    }
}
