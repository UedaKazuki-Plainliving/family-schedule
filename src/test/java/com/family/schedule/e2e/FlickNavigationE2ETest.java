package com.family.schedule.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * flick_navigation.feature 相当 — フリック操作による日付移動の動作検証
 */
class FlickNavigationE2ETest extends BaseE2ETest {

    @BeforeEach
    void navigateToSchedule() {
        setCurrentUser("長女", 3);
        page.navigate(baseUrl() + "/");
        assertThat(page.locator("#screen-schedule")).isVisible();
    }

    /** セルが指定日付で存在することを確認する */
    private void assertDateVisible(String date) {
        assertThat(page.locator(".schedule-cell[data-date='" + date + "']").first()).isVisible();
    }

    /** セルが指定日付で存在しないことを確認する */
    private void assertDateHidden(String date) {
        assertThat(page.locator(".schedule-cell[data-date='" + date + "']")).hasCount(0);
    }

    @Test
    void 左フリックで翌日へ進む() {
        flickLeft();
        assertDateVisible(tomorrow());
        assertDateVisible(dayAfterTomorrow());
        assertDateHidden(today());
    }

    @Test
    void 右フリックで前日へ戻る() {
        flickRight();
        assertDateVisible(daysLater(-1));
        assertDateVisible(today());
        assertDateHidden(tomorrow());
    }

    @Test
    void 連続フリックで複数日先へ進める() {
        flickLeft();
        flickLeft();
        flickLeft();
        assertDateVisible(daysLater(3));
        assertDateVisible(daysLater(4));
        assertDateHidden(today());
    }

    @Test
    void 今日に戻るボタンで初期表示に戻る() {
        // 5回フリックして遠ざかる
        for (int i = 0; i < 5; i++) flickLeft();
        assertDateHidden(today());

        // 今日に戻る
        page.locator("#btn-today").click();
        page.waitForTimeout(400);

        assertDateVisible(today());
        assertDateVisible(tomorrow());
    }

    @Test
    void 過去の日付にも遡れる() {
        flickRight();
        flickRight();
        flickRight();
        assertDateVisible(daysLater(-3));
        assertDateVisible(daysLater(-2));
        assertDateHidden(today());
    }
}
