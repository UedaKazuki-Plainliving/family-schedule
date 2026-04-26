package com.family.schedule.e2e;

import com.family.schedule.e2e.page.AddScheduleModal;
import com.family.schedule.e2e.page.SchedulePage;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class CreateScheduleE2ETest extends BaseE2ETest {

    @Test
    void 必要最小限の入力で登録できる() {
        SchedulePage sp = schedulePage();
        AddScheduleModal modal = addScheduleModal();
        setCurrentUser(Members.MOM);
        page.navigate(baseUrl() + "/");

        sp.clickAdd();
        modal.selectWho(Members.DAUGHTER1.name);
        modal.setDate(tomorrow());
        modal.setContent("塾");
        modal.save();

        assertThat(modal.modal()).isHidden();
        assertThat(sp.scheduleItemText(Members.DAUGHTER1, tomorrow(), "塾")).isVisible();
    }

    @Test
    void フォームを開いたときの初期値が正しい() {
        AddScheduleModal modal = addScheduleModal();
        setCurrentUser(Members.MOM);
        page.navigate(baseUrl() + "/");

        schedulePage().clickAdd();

        assertThat(modal.selectedWho()).hasText(Members.MOM.name);
        assertThat(modal.dateInput()).hasValue(today());
        assertThat(modal.contentInput()).hasValue("");
    }

    @Test
    void 誰がはメンバーボタンのワンタップ切替() {
        AddScheduleModal modal = addScheduleModal();
        setCurrentUser(Members.MOM);
        page.navigate(baseUrl() + "/");

        schedulePage().clickAdd();

        for (Members m : Members.values()) {
            assertThat(modal.whoButton(m.name)).isVisible();
        }

        modal.selectWho(Members.SON1.name);
        assertThat(modal.selectedWho()).hasText(Members.SON1.name);
    }

    @Test
    void 登録は3ステップ以内で完了する_FR21() {
        SchedulePage sp = schedulePage();
        AddScheduleModal modal = addScheduleModal();
        setCurrentUser(Members.MOM);
        page.navigate(baseUrl() + "/");

        sp.clickAdd();
        modal.selectWho(Members.SON1.name);
        modal.setContent("サッカー");
        modal.save();

        assertThat(sp.scheduleItemText(Members.SON1, today(), "サッカー")).isVisible();
    }

    @Test
    void 内容が空のときはエラーが表示される() {
        AddScheduleModal modal = addScheduleModal();
        setCurrentUser(Members.MOM);
        page.navigate(baseUrl() + "/");

        schedulePage().clickAdd();
        modal.setContent("");
        modal.save();

        assertThat(modal.errorMessage()).hasText("内容を入力してください");
        assertThat(modal.modal()).isVisible();
    }

    @Test
    void 内容が100文字を超える場合はエラーが表示される() {
        AddScheduleModal modal = addScheduleModal();
        setCurrentUser(Members.MOM);
        page.navigate(baseUrl() + "/");

        schedulePage().clickAdd();
        modal.setContent("あ".repeat(101));
        modal.save();

        assertThat(modal.errorMessage()).hasText("内容は100文字以内で入力してください");
        assertThat(modal.modal()).isVisible();
    }

    @Test
    void 保存に成功するとトーストが表示される() {
        SchedulePage sp = schedulePage();
        AddScheduleModal modal = addScheduleModal();
        setCurrentUser(Members.MOM);
        page.navigate(baseUrl() + "/");

        sp.clickAdd();
        modal.selectWho(Members.DAUGHTER1.name);
        modal.setDate(tomorrow());
        modal.setContent("塾");
        modal.save();

        assertThat(sp.toast()).hasText("保存しました");
    }

    @Test
    void キャンセルすると保存されない() {
        SchedulePage sp = schedulePage();
        AddScheduleModal modal = addScheduleModal();
        setCurrentUser(Members.MOM);
        page.navigate(baseUrl() + "/");

        sp.clickAdd();
        modal.selectWho(Members.DAUGHTER2.name);
        modal.setContent("ピアノ");
        modal.cancel();

        assertThat(modal.modal()).isHidden();
        assertThat(schedulePage().allScheduleItemTexts("ピアノ")).hasCount(0);
    }
}
