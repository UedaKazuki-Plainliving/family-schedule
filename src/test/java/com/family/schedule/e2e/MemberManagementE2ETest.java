package com.family.schedule.e2e;

import com.family.schedule.e2e.page.MemberManagePage;
import com.family.schedule.e2e.page.SchedulePage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class MemberManagementE2ETest extends BaseE2ETest {

    @BeforeEach
    @Override
    void setUp(TestInfo info) {
        super.setUp(info);
        jdbc.execute("DELETE FROM members");
        jdbc.execute("INSERT INTO members (id, name, display_order) VALUES " +
                "(1,'お父さん',1),(2,'お母さん',2),(3,'長女',3),(4,'次女',4),(5,'長男',5)");
    }

    private void openMemberModal() {
        setCurrentUser(Members.DAD);
        page.navigate(baseUrl() + "/");
        assertThat(schedulePage().screen()).isVisible();
        schedulePage().clickMemberSettings();
        assertThat(memberManagePage().modal()).isVisible();
    }

    @Test
    void メンバーを追加できる() {
        MemberManagePage mp = memberManagePage();
        openMemberModal();

        mp.addMember("おじいちゃん");
        page.waitForTimeout(500);

        assertThat(mp.memberItem("おじいちゃん")).isVisible();
    }

    @Test
    void メンバーの名前を変更できる() {
        MemberManagePage mp = memberManagePage();
        openMemberModal();

        mp.clickRename(Members.DAUGHTER1.name);
        mp.submitRename("さくら");
        page.waitForTimeout(500);

        assertThat(mp.memberItem("さくら")).isVisible();
        assertThat(mp.memberItem(Members.DAUGHTER1.name)).hasCount(0);
    }

    @Test
    void 予定がないメンバーを削除できる() {
        MemberManagePage mp = memberManagePage();
        openMemberModal();

        mp.deleteMember(Members.DAUGHTER2.name);
        page.waitForTimeout(500);

        assertThat(mp.memberItem(Members.DAUGHTER2.name)).hasCount(0);
    }

    @Test
    void 予定があるメンバーは削除できない() {
        insertSchedule(Members.SON1, today(), "サッカー教室");
        MemberManagePage mp = memberManagePage();
        openMemberModal();

        mp.deleteMember(Members.SON1.name);
        page.waitForTimeout(500);

        assertThat(mp.errorMessage()).isVisible();
        assertThat(mp.memberItem(Members.SON1.name)).isVisible();
    }

    @AfterEach
    void resetMembers() {
        jdbc.execute("DELETE FROM schedules");
        jdbc.execute("DELETE FROM members");
        jdbc.execute("INSERT INTO members (id, name, display_order) VALUES " +
                "(1,'お父さん',1),(2,'お母さん',2),(3,'長女',3),(4,'次女',4),(5,'長男',5)");
    }

    @Test
    void メンバー10名を超えて追加できない() {
        for (int i = 6; i <= 10; i++) {
            jdbc.update("INSERT INTO members (id, name, display_order) VALUES (?, ?, ?)", i, "テスト" + i, i);
        }
        MemberManagePage mp = memberManagePage();
        openMemberModal();

        assertThat(mp.addSection()).isHidden();
    }

    @Test
    void 名前が空では追加できない() {
        MemberManagePage mp = memberManagePage();
        openMemberModal();

        mp.addMember("");
        page.waitForTimeout(300);

        assertThat(mp.errorMessage()).isVisible();
    }

    @Test
    void 重複した名前は追加できない() {
        MemberManagePage mp = memberManagePage();
        openMemberModal();

        mp.addMember(Members.DAD.name);
        page.waitForTimeout(500);

        assertThat(mp.errorMessage()).isVisible();
    }
}
