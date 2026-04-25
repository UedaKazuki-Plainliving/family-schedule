package com.family.schedule.repository;

import com.family.schedule.domain.Schedule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ScheduleRepositoryTest {

    @Autowired
    ScheduleRepository repo;

    @Autowired
    MemberRepository memberRepo;

    @Test
    void 予定0件のとき空リスト() {
        List<Schedule> result = repo.findByDateBetweenAndDeletedAtIsNullOrderByDateAscMemberIdAscIdAsc(
                LocalDate.of(2026, 4, 24), LocalDate.of(2026, 4, 25));
        assertThat(result).isEmpty();
    }

    @Test
    void 日付範囲内のものだけ取得され日付昇順メンバー順() {
        assertThat(memberRepo.count()).isEqualTo(5);

        repo.save(new Schedule(3, LocalDate.of(2026, 4, 24), "部活"));
        repo.save(new Schedule(1, LocalDate.of(2026, 4, 24), "在宅"));
        repo.save(new Schedule(2, LocalDate.of(2026, 4, 25), "病院"));
        repo.save(new Schedule(1, LocalDate.of(2026, 4, 23), "範囲外"));
        repo.save(new Schedule(1, LocalDate.of(2026, 4, 26), "範囲外"));

        List<Schedule> result = repo.findByDateBetweenAndDeletedAtIsNullOrderByDateAscMemberIdAscIdAsc(
                LocalDate.of(2026, 4, 24), LocalDate.of(2026, 4, 25));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2026, 4, 24));
        assertThat(result.get(0).getMemberId()).isEqualTo(1); // お父さん
        assertThat(result.get(1).getMemberId()).isEqualTo(3); // 長女
        assertThat(result.get(2).getDate()).isEqualTo(LocalDate.of(2026, 4, 25));
    }
}
