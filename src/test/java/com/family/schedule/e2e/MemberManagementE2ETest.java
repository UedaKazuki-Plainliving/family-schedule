package com.family.schedule.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * member_management.feature 相当 — メンバー管理（FR-24）の動作検証
 */
class MemberManagementE2ETest extends BaseE2ETest {

    @BeforeEach
    @Override
    void setUp(TestInfo info) {
        super.setUp(info);
        // メンバーテーブルを初期5名にリセット（schedules は super.setUp で削除済み）
        jdbc.execute("DELETE FROM members");
        jdbc.execute("INSERT INTO members (id, name, display_order) VALUES " +
                "(1,'お父さん',1),(2,'お母さん',2),(3,'長女',3),(4,'次女',4),(5,'長男',5)");
    }

    private void openMemberModal() {
        setCurrentUser("お父さん", 1);
        page.navigate(baseUrl() + "/");
        assertThat(page.locator("#screen-schedule")).isVisible();
        page.locator("#btn-member-settings").click();
        assertThat(page.locator("#member-modal")).isVisible();
    }

    private com.microsoft.playwright.Locator memberItem(String name) {
        return page.locator(".member-manage-item").filter(
                new com.microsoft.playwright.Locator.FilterOptions().setHasText(name));
    }

    @Test
    void メンバーを追加できる() {
        openMemberModal();

        page.locator("#member-add-input").fill("おじいちゃん");
        page.locator("#btn-member-add").click();
        page.waitForTimeout(500);

        assertThat(memberItem("おじいちゃん")).isVisible();
    }

    @Test
    void メンバーの名前を変更できる() {
        openMemberModal();

        memberItem("長女").locator("button:has-text('変更')").click();
        page.locator(".member-rename-input").fill("さくら");
        page.locator(".member-rename-input").press("Enter");
        page.waitForTimeout(500);

        assertThat(memberItem("さくら")).isVisible();
        assertThat(memberItem("長女")).hasCount(0);
    }

    @Test
    void 予定がないメンバーを削除できる() {
        openMemberModal();

        memberItem("次女").locator("button:has-text('削除')").click();
        page.waitForTimeout(500);

        assertThat(memberItem("次女")).hasCount(0);
    }

    @Test
    void 予定があるメンバーは削除できない() {
        insertSchedule(5, today(), "サッカー教室"); // 長男(5)
        openMemberModal();

        memberItem("長男").locator("button:has-text('削除')").click();
        page.waitForTimeout(500);

        assertThat(page.locator("#member-modal-error")).isVisible();
        assertThat(memberItem("長男")).isVisible();
    }

    @AfterEach
    void resetMembers() {
        // テスト後のDB状態を初期5名に戻す（後続テストクラスへの影響を防ぐ）
        jdbc.execute("DELETE FROM schedules");
        jdbc.execute("DELETE FROM members");
        jdbc.execute("INSERT INTO members (id, name, display_order) VALUES " +
                "(1,'お父さん',1),(2,'お母さん',2),(3,'長女',3),(4,'次女',4),(5,'長男',5)");
    }

    @Test
    void メンバー10名を超えて追加できない() {
        // 既存5名に5名追加して計10名にする（H2はINTEGER PRIMARY KEYを自動採番しないため明示指定）
        for (int i = 6; i <= 10; i++) {
            jdbc.update("INSERT INTO members (id, name, display_order) VALUES (?, ?, ?)", i, "テスト" + i, i);
        }
        openMemberModal();

        // 10名到達時は追加セクションが非表示になる
        assertThat(page.locator("#member-add-section")).isHidden();
    }

    @Test
    void 名前が空では追加できない() {
        openMemberModal();

        page.locator("#member-add-input").fill("");
        page.locator("#btn-member-add").click();
        page.waitForTimeout(300);

        assertThat(page.locator("#member-modal-error")).isVisible();
    }

    @Test
    void 重複した名前は追加できない() {
        openMemberModal();

        page.locator("#member-add-input").fill("お父さん");
        page.locator("#btn-member-add").click();
        page.waitForTimeout(500);

        assertThat(page.locator("#member-modal-error")).isVisible();
    }
}
