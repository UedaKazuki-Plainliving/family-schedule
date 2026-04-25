package com.family.schedule.api;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseApiTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbc;

    protected static Playwright playwright;
    protected APIRequestContext api;

    @BeforeAll
    static void initPlaywright() {
        // API テストはブラウザ不要なのでダウンロードをスキップ
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(java.util.Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
    }

    @AfterAll
    static void closePlaywright() {
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void setUpApi() {
        jdbc.execute("DELETE FROM schedules");
        jdbc.execute("ALTER TABLE schedules ALTER COLUMN id RESTART WITH 1");
        api = playwright.request().newContext(
                new com.microsoft.playwright.APIRequest.NewContextOptions()
                        .setBaseURL("http://localhost:" + port));
    }

    protected APIResponse get(String path) {
        return api.get(path);
    }
    protected APIResponse post(String path, Object body) {
        return api.post(path, RequestOptions.create().setData(body));
    }
    protected APIResponse put(String path, Object body) {
        return api.put(path, RequestOptions.create().setData(body));
    }
    protected APIResponse delete(String path) {
        return api.delete(path);
    }
}
