package com.family.schedule.e2e;

import com.family.schedule.e2e.page.SelectUserPage;
import com.family.schedule.e2e.page.SchedulePage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class SelectUserE2ETest extends BaseE2ETest {

    @Test
    void 登録メンバーの名前ボタンが全員分表示される() {
        SelectUserPage sup = selectUserPage();
        page.navigate(baseUrl() + "/");
        assertThat(sup.screen()).isVisible();
        for (String name : List.of("お父さん", "お母さん", "長女", "次女", "長男")) {
            assertThat(sup.memberButton(name)).isVisible();
        }
    }

    @ParameterizedTest(name = "{0} を選ぶとスケジュール画面に遷移する")
    @CsvSource({
        "お父さん, 1",
        "お母さん, 2",
        "長女,     3",
        "次女,     4",
        "長男,     5"
    })
    void 名前を選ぶとスケジュール画面に遷移する(String name, int id) {
        SelectUserPage sup = selectUserPage();
        SchedulePage sp = schedulePage();
        page.navigate(baseUrl() + "/");
        sup.selectUser(name);
        assertThat(sp.screen()).isVisible();
        assertThat(sp.currentUserName()).hasText(name);
    }
}
