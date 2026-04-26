package com.family.schedule.e2e.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class AddScheduleModal {

    private final Page page;

    public AddScheduleModal(Page page) {
        this.page = page;
    }

    public Locator modal() {
        return page.locator("#modal");
    }

    public Locator title() {
        return page.locator("#form-title");
    }

    public Locator errorMessage() {
        return page.locator("#error-msg");
    }

    public Locator dateInput() {
        return page.locator("#date-input");
    }

    public Locator contentInput() {
        return page.locator("#content-input");
    }

    public Locator selectedWho() {
        return page.locator("#who-btns .who-btn.selected");
    }

    public Locator whoButton(String name) {
        return page.locator("#who-btns .who-btn").filter(
                new Locator.FilterOptions().setHasText(name));
    }

    public void selectWho(String name) {
        whoButton(name).click();
    }

    public void setDate(String date) {
        page.locator("#date-input").fill(date);
    }

    public void setContent(String content) {
        page.locator("#content-input").fill(content);
    }

    public void save() {
        page.locator("#btn-save").click();
    }

    public void cancel() {
        page.locator("#btn-cancel").click();
    }

    public void close() {
        page.locator("#btn-close").click();
    }
}
