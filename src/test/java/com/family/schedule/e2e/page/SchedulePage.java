package com.family.schedule.e2e.page;

import com.family.schedule.e2e.Members;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class SchedulePage {

    private final Page page;

    public SchedulePage(Page page) {
        this.page = page;
    }

    public Locator screen() {
        return page.locator("#screen-schedule");
    }

    public Locator currentUserName() {
        return page.locator("#current-user-name");
    }

    public Locator dateHeadingLeft() {
        return page.locator("#date-heading-left");
    }

    public Locator dateHeadingRight() {
        return page.locator("#date-heading-right");
    }

    public Locator btnToday() {
        return page.locator("#btn-today");
    }

    public Locator toast() {
        return page.locator("#toast");
    }

    public Locator scheduleCell(Members member, String date) {
        return scheduleCell(member.id, date);
    }

    public Locator scheduleCell(int memberId, String date) {
        return page.locator(
                ".schedule-cell[data-member-id='" + memberId + "'][data-date='" + date + "']");
    }

    public Locator scheduleItemText(Members member, String date, String content) {
        return scheduleItemText(member.id, date, content);
    }

    public Locator scheduleItemText(int memberId, String date, String content) {
        return scheduleCell(memberId, date)
                .locator(".schedule-item-text:has-text('" + content + "')");
    }

    public Locator firstScheduleItem(Members member, String date) {
        return firstScheduleItem(member.id, date);
    }

    public Locator firstScheduleItem(int memberId, String date) {
        return scheduleCell(memberId, date).locator(".schedule-item").first();
    }

    public Locator noScheduleInCell(Members member, String date) {
        return noScheduleInCell(member.id, date);
    }

    public Locator noScheduleInCell(int memberId, String date) {
        return scheduleCell(memberId, date).locator(".schedule-none");
    }

    public Locator noScheduleForDate(String date) {
        return page.locator("[data-date='" + date + "'] .schedule-none").first();
    }

    public Locator cellsForDate(String date) {
        return page.locator("[data-date='" + date + "']");
    }

    public Locator inlineEditInput() {
        return page.locator(".schedule-item-input");
    }

    public void clickAdd() {
        page.locator("#btn-add").click();
    }

    public void clickSwitchUser() {
        page.locator("#btn-switch-user").click();
    }

    public void clickToday() {
        page.locator("#btn-today").click();
    }

    public void clickMemberSettings() {
        page.locator("#btn-member-settings").click();
    }

    public Locator allScheduleItemTexts(String content) {
        return page.locator(".schedule-item-text:has-text('" + content + "')");
    }

    public Locator scheduleItemByText(String content) {
        return page.locator(".schedule-item:has-text('" + content + "')");
    }

    public Locator firstScheduleItemGlobal() {
        return page.locator(".schedule-item").first();
    }

    public void flickLeft() {
        page.mouse().move(300, 250);
        page.mouse().down();
        page.mouse().move(100, 250);
        page.mouse().up();
        page.waitForTimeout(500);
    }

    public void flickRight() {
        page.mouse().move(100, 250);
        page.mouse().down();
        page.mouse().move(300, 250);
        page.mouse().up();
        page.waitForTimeout(500);
    }
}
