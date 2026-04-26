package com.family.schedule.e2e.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class SelectUserPage {

    private final Page page;

    public SelectUserPage(Page page) {
        this.page = page;
    }

    public Locator screen() {
        return page.locator("#screen-select-user");
    }

    public Locator memberButton(String name) {
        return page.locator(".member-btn").filter(
                new Locator.FilterOptions().setHasText(name));
    }

    public void selectUser(String name) {
        memberButton(name).click();
    }
}
