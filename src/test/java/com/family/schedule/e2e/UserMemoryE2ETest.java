package com.family.schedule.e2e;

import com.family.schedule.e2e.page.SelectUserPage;
import com.family.schedule.e2e.page.SchedulePage;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class UserMemoryE2ETest extends BaseE2ETest {

    @Test
    void 初回起動時は利用者選択画面が出る() {
        SelectUserPage sup = selectUserPage();
        SchedulePage sp = schedulePage();
        page.navigate(baseUrl() + "/");
        assertThat(sup.screen()).isVisible();
        assertThat(sp.screen()).isHidden();
    }

    @Test
    void 利用者を選択すると端末に記憶される() {
        SelectUserPage sup = selectUserPage();
        SchedulePage sp = schedulePage();
        page.navigate(baseUrl() + "/");
        sup.selectUser("お母さん");

        assertThat(sp.screen()).isVisible();
        assertThat(sp.currentUserName()).hasText("お母さん");

        Object stored = page.evaluate("localStorage.getItem('familySchedule.currentUser')");
        org.junit.jupiter.api.Assertions.assertNotNull(stored);
        org.junit.jupiter.api.Assertions.assertTrue(stored.toString().contains("お母さん"));
    }

    @Test
    void 二回目以降はスケジュール画面から始まる() {
        SelectUserPage sup = selectUserPage();
        SchedulePage sp = schedulePage();
        setCurrentUser("お母さん", 2);
        page.navigate(baseUrl() + "/");

        assertThat(sp.screen()).isVisible();
        assertThat(sup.screen()).isHidden();
        assertThat(sp.currentUserName()).hasText("お母さん");
    }
}
