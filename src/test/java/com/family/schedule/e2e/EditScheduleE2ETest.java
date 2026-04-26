package com.family.schedule.e2e;

import com.family.schedule.e2e.page.SchedulePage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class EditScheduleE2ETest extends BaseE2ETest {

    @BeforeEach
    void insertFixture() {
        insertSchedule(3, today(), "部活"); // 長女(3)
        setCurrentUser("お父さん", 1);
    }

    @Test
    void 予定をタップするとインライン入力欄が開く() {
        SchedulePage sp = schedulePage();
        page.navigate(baseUrl() + "/");

        sp.firstScheduleItem(3, today()).click();

        assertThat(sp.inlineEditInput()).isVisible();
        assertThat(sp.inlineEditInput()).hasValue("部活");
    }

    @Test
    void 内容を書き換えてEnterで保存する() {
        SchedulePage sp = schedulePage();
        page.navigate(baseUrl() + "/");

        sp.firstScheduleItem(3, today()).click();
        sp.inlineEditInput().fill("部活（19時まで）");
        sp.inlineEditInput().press("Enter");
        page.waitForTimeout(600);

        assertThat(sp.inlineEditInput()).hasCount(0);
        assertThat(sp.scheduleItemText(3, today(), "部活（19時まで）")).isVisible();
        assertThat(sp.toast()).hasText("更新しました");
    }

    @Test
    void Escapeでキャンセルすると元の内容に戻る() {
        SchedulePage sp = schedulePage();
        page.navigate(baseUrl() + "/");

        sp.firstScheduleItem(3, today()).click();
        sp.inlineEditInput().fill("変更途中");
        sp.inlineEditInput().press("Escape");
        page.waitForTimeout(200);

        assertThat(sp.inlineEditInput()).hasCount(0);
        assertThat(sp.scheduleItemText(3, today(), "部活")).isVisible();
    }

    @Test
    void 内容を空にしてEnterを押すと変更がキャンセルされる() {
        SchedulePage sp = schedulePage();
        page.navigate(baseUrl() + "/");

        sp.firstScheduleItem(3, today()).click();
        sp.inlineEditInput().fill("");
        sp.inlineEditInput().press("Enter");
        page.waitForTimeout(300);

        assertThat(sp.inlineEditInput()).hasCount(0);
        assertThat(sp.scheduleItemText(3, today(), "部活")).isVisible();
    }

    @Test
    void 長押しではインライン編集モードにならない_FR23() {
        SchedulePage sp = schedulePage();
        page.navigate(baseUrl() + "/");

        var item = sp.firstScheduleItem(3, today());
        item.hover();
        page.mouse().down();
        page.waitForTimeout(500);
        page.mouse().up();

        assertThat(sp.inlineEditInput()).hasCount(0);
    }
}
