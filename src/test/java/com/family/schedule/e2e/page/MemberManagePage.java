package com.family.schedule.e2e.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class MemberManagePage {

    private final Page page;

    public MemberManagePage(Page page) {
        this.page = page;
    }

    public Locator modal() {
        return page.locator("#member-modal");
    }

    public Locator errorMessage() {
        return page.locator("#member-modal-error");
    }

    public Locator addSection() {
        return page.locator("#member-add-section");
    }

    public Locator memberItem(String name) {
        return page.locator(".member-manage-item").filter(
                new Locator.FilterOptions().setHasText(name));
    }

    public void addMember(String name) {
        page.locator("#member-add-input").fill(name);
        page.locator("#btn-member-add").click();
    }

    public void clickRename(String name) {
        memberItem(name).locator("button:has-text('変更')").click();
    }

    public void submitRename(String newName) {
        page.locator(".member-rename-input").fill(newName);
        page.locator(".member-rename-input").press("Enter");
    }

    public void deleteMember(String name) {
        memberItem(name).locator("button:has-text('削除')").click();
    }
}
