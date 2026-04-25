package com.family.schedule.e2e;

import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * view_schedule.feature 相当 — 今日・明日のスケジュール表示検証
 */
class ViewScheduleE2ETest extends BaseE2ETest {

    @Test
    void 予定が登録されていないときは全員分予定なしが表示される() {
        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");
        assertThat(page.locator("#screen-schedule")).isVisible();

        // 今日・明日の全メンバーセルに「予定なし」が表示される
        assertThat(page.locator("[data-date='" + today() + "'] .schedule-none").first())
                .hasText("予定なし");
        assertThat(page.locator("[data-date='" + tomorrow() + "'] .schedule-none").first())
                .hasText("予定なし");
    }

    @Test
    void 今日と明日の予定が人ごとに表示される() {
        // お父さん(1)・長女(3)の今日分、次女(4)・長男(5)の明日分を登録
        insertSchedule(1, today(), "在宅勤務");
        insertSchedule(3, today(), "部活");
        insertSchedule(4, tomorrow(), "スイミング");
        insertSchedule(5, tomorrow(), "サッカー教室");

        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");

        assertThat(cell(1, today()).locator(".schedule-item-text:has-text('在宅勤務')")).isVisible();
        assertThat(cell(3, today()).locator(".schedule-item-text:has-text('部活')")).isVisible();
        assertThat(cell(4, tomorrow()).locator(".schedule-item-text:has-text('スイミング')")).isVisible();
        assertThat(cell(5, tomorrow()).locator(".schedule-item-text:has-text('サッカー教室')")).isVisible();
    }

    @Test
    void 同じ人の同じ日に複数の予定があるとすべて表示される() {
        insertSchedule(3, today(), "部活");
        insertSchedule(3, today(), "塾");

        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");

        assertThat(cell(3, today()).locator(".schedule-item-text:has-text('部活')")).isVisible();
        assertThat(cell(3, today()).locator(".schedule-item-text:has-text('塾')")).isVisible();
        assertThat(cell(3, today()).locator(".schedule-item")).hasCount(2);
    }
}
