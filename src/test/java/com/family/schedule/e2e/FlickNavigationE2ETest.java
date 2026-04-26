package com.family.schedule.e2e;

import com.family.schedule.e2e.page.SchedulePage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class FlickNavigationE2ETest extends BaseE2ETest {

    private SchedulePage sp;

    @BeforeEach
    void navigateToSchedule() {
        sp = schedulePage();
        setCurrentUser(Members.DAUGHTER1);
        page.navigate(baseUrl() + "/");
        assertThat(sp.screen()).isVisible();
    }

    private void assertDateVisible(String date) {
        assertThat(sp.cellsForDate(date).first()).isVisible();
    }

    private void assertDateHidden(String date) {
        assertThat(sp.cellsForDate(date)).hasCount(0);
    }

    @Test
    void 左フリックで翌日へ進む() {
        sp.flickLeft();
        assertDateVisible(tomorrow());
        assertDateVisible(dayAfterTomorrow());
        assertDateHidden(today());
    }

    @Test
    void 右フリックで前日へ戻る() {
        sp.flickRight();
        assertDateVisible(daysLater(-1));
        assertDateVisible(today());
        assertDateHidden(tomorrow());
    }

    @Test
    void 連続フリックで複数日先へ進める() {
        sp.flickLeft();
        sp.flickLeft();
        sp.flickLeft();
        assertDateVisible(daysLater(3));
        assertDateVisible(daysLater(4));
        assertDateHidden(today());
    }

    @Test
    void 今日に戻るボタンで初期表示に戻る() {
        for (int i = 0; i < 5; i++) sp.flickLeft();
        assertDateHidden(today());

        sp.clickToday();
        page.waitForTimeout(400);

        assertDateVisible(today());
        assertDateVisible(tomorrow());
    }

    @Test
    void 過去の日付にも遡れる() {
        sp.flickRight();
        sp.flickRight();
        sp.flickRight();
        assertDateVisible(daysLater(-3));
        assertDateVisible(daysLater(-2));
        assertDateHidden(today());
    }
}
