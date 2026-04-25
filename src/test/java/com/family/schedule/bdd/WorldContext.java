package com.family.schedule.bdd;

import com.microsoft.playwright.Page;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
@ScenarioScope
public class WorldContext {

    public Page page;
    public int port;
    public LocalDate featureToday = LocalDate.now();
    public LocalDate viewDate = LocalDate.now();
    public int stepCount = 0;

    public String baseUrl() {
        return "http://localhost:" + port;
    }

    /** feature-file の日付を featureToday→real today のオフセットで実日付に変換する */
    public String resolveDate(String featureDateStr) {
        LocalDate fd = LocalDate.parse(featureDateStr);
        long offset = ChronoUnit.DAYS.between(featureToday, fd);
        return LocalDate.now().plusDays(offset).toString();
    }

    public String today() {
        return LocalDate.now().toString();
    }

    public String tomorrow() {
        return LocalDate.now().plusDays(1).toString();
    }

    public String dayLabel(LocalDate d) {
        return d.getMonthValue() + "/" + d.getDayOfMonth();
    }
}
