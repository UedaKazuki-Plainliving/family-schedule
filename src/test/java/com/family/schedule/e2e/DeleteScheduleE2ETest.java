package com.family.schedule.e2e;

import com.family.schedule.e2e.page.SchedulePage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class DeleteScheduleE2ETest extends BaseE2ETest {

    @BeforeEach
    void insertFixture() {
        insertSchedule(Members.SON1, today(), "サッカー教室");
        setCurrentUser(Members.MOM);
    }

    @Test
    void バツボタンで予定を即削除する() {
        SchedulePage sp = schedulePage();
        page.navigate(baseUrl() + "/");

        sp.firstScheduleItem(Members.SON1, today()).locator(".schedule-item-delete").click();
        page.waitForTimeout(500);

        assertThat(sp.noScheduleInCell(Members.SON1, today())).isVisible();
        assertThat(sp.toast()).containsText("削除しました");
    }

    @Test
    void 削除後に元に戻すで復元できる_FR27_UNDO() {
        SchedulePage sp = schedulePage();
        page.navigate(baseUrl() + "/");

        sp.firstScheduleItem(Members.SON1, today()).locator(".schedule-item-delete").click();
        page.waitForTimeout(500);

        assertThat(sp.toast()).containsText("削除しました");

        sp.toast().locator("button:has-text('元に戻す')").click();
        page.waitForTimeout(1200);

        assertThat(sp.scheduleItemText(Members.SON1, today(), "サッカー教室")).isVisible();
        assertThat(sp.toast()).hasText("元に戻しました");
    }
}
