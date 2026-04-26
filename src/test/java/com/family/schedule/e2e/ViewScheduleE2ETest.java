package com.family.schedule.e2e;

import com.family.schedule.e2e.page.SchedulePage;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class ViewScheduleE2ETest extends BaseE2ETest {

    @Test
    void 予定が登録されていないときは全員分予定なしが表示される() {
        SchedulePage sp = schedulePage();
        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");
        assertThat(sp.screen()).isVisible();

        assertThat(sp.noScheduleForDate(today())).hasText("予定なし");
        assertThat(sp.noScheduleForDate(tomorrow())).hasText("予定なし");
    }

    @Test
    void 今日と明日の予定が人ごとに表示される() {
        insertSchedule(1, today(), "在宅勤務");
        insertSchedule(3, today(), "部活");
        insertSchedule(4, tomorrow(), "スイミング");
        insertSchedule(5, tomorrow(), "サッカー教室");

        SchedulePage sp = schedulePage();
        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");

        assertThat(sp.scheduleItemText(1, today(), "在宅勤務")).isVisible();
        assertThat(sp.scheduleItemText(3, today(), "部活")).isVisible();
        assertThat(sp.scheduleItemText(4, tomorrow(), "スイミング")).isVisible();
        assertThat(sp.scheduleItemText(5, tomorrow(), "サッカー教室")).isVisible();
    }

    @Test
    void 同じ人の同じ日に複数の予定があるとすべて表示される() {
        insertSchedule(3, today(), "部活");
        insertSchedule(3, today(), "塾");

        SchedulePage sp = schedulePage();
        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");

        assertThat(sp.scheduleItemText(3, today(), "部活")).isVisible();
        assertThat(sp.scheduleItemText(3, today(), "塾")).isVisible();
        assertThat(sp.scheduleCell(3, today()).locator(".schedule-item")).hasCount(2);
    }
}
