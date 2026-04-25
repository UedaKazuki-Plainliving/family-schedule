package com.family.schedule.explore;

import com.family.schedule.e2e.BaseE2ETest;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class XssExploreTest extends BaseE2ETest {

    @Test
    void EX003_T1_XSSペイロードが画面で発火しない() {
        // <script>を埋め込み
        context.request().post(baseUrl() + "/api/schedules",
                RequestOptions.create().setData(Map.of(
                        "memberId", 1, "date", java.time.LocalDate.now().toString(),
                        "content", "<script>window.__xss=true;alert('XSS')</script>")));

        AtomicBoolean dialogFired = new AtomicBoolean(false);
        page.onDialog(d -> { dialogFired.set(true); d.accept(); });

        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser','{\"id\":1,\"name\":\"お父さん\",\"displayOrder\":1}');");
        page.navigate(baseUrl() + "/");
        page.waitForTimeout(1500);

        // xss が発火しなかったこと
        assertThat(dialogFired.get()).isFalse();
        Object xss = page.evaluate("() => window.__xss === true");
        assertThat(xss).isEqualTo(false);

        // ただし内容はそのまま表示されている（テキストとして）
        String txt = page.locator(".schedule-item").first().innerText();
        assertThat(txt).contains("<script>");

        snap("XSS_safe_textContent");
    }

    @Test
    void EX003_T2_imgタグもテキストとして無害に表示される() {
        context.request().post(baseUrl() + "/api/schedules",
                RequestOptions.create().setData(Map.of(
                        "memberId", 1, "date", java.time.LocalDate.now().toString(),
                        "content", "<img src=x onerror=window.__xss2=true>")));

        context.addInitScript(
                "localStorage.setItem('familySchedule.currentUser','{\"id\":1,\"name\":\"お父さん\",\"displayOrder\":1}');");
        page.navigate(baseUrl() + "/");
        page.waitForTimeout(1500);

        Object xss = page.evaluate("() => window.__xss2 === true");
        assertThat(xss).isEqualTo(false);
        assertThat(page.locator(".schedule-item").first().innerText()).contains("<img");
    }
}
