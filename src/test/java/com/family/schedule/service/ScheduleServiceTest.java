package com.family.schedule.service;

import com.family.schedule.domain.Schedule;
import com.family.schedule.repository.ScheduleRepository;
import com.family.schedule.web.dto.ScheduleRequest;
import com.family.schedule.web.dto.ScheduleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleServiceTest {

    private ScheduleRepository repo;
    private MemberService memberService;
    private ScheduleService service;

    @BeforeEach
    void setUp() {
        repo = mock(ScheduleRepository.class);
        memberService = mock(MemberService.class);
        when(memberService.nameById()).thenReturn(Map.of(
                1, "お父さん", 2, "お母さん", 3, "そよ", 4, "ゆうり", 5, "いちろう"));
        service = new ScheduleService(repo, memberService);
    }

    @Test
    void findRange_正常() {
        Schedule s = new Schedule(3, LocalDate.of(2026, 4, 24), "部活");
        setId(s, 10L);
        when(repo.findByDateBetweenAndDeletedAtIsNullOrderByDateAscMemberIdAscIdAsc(
                LocalDate.of(2026, 4, 24), LocalDate.of(2026, 4, 25)))
                .thenReturn(List.of(s));

        List<ScheduleResponse> result = service.findRange(
                LocalDate.of(2026, 4, 24), LocalDate.of(2026, 4, 25));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).memberName()).isEqualTo("そよ");
        assertThat(result.get(0).content()).isEqualTo("部活");
    }

    @Test
    void findRange_from超えtoはエラー() {
        assertThatThrownBy(() -> service.findRange(
                LocalDate.of(2026, 4, 25), LocalDate.of(2026, 4, 24)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void findRange_null引数はエラー() {
        assertThatThrownBy(() -> service.findRange(null, LocalDate.now()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_正常() {
        ScheduleRequest req = new ScheduleRequest(3, LocalDate.of(2026, 4, 25), "塾");
        ArgumentCaptor<Schedule> cap = ArgumentCaptor.forClass(Schedule.class);
        when(repo.save(cap.capture())).thenAnswer(inv -> {
            Schedule saved = inv.getArgument(0);
            setId(saved, 42L);
            return saved;
        });

        ScheduleResponse r = service.create(req);

        assertThat(r.id()).isEqualTo(42L);
        assertThat(r.memberName()).isEqualTo("そよ");
        assertThat(cap.getValue().getContent()).isEqualTo("塾");
    }

    @Test
    void create_contentは前後スペースがトリムされる() {
        when(repo.save(any(Schedule.class))).thenAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            setId(s, 1L);
            return s;
        });
        ScheduleResponse r = service.create(new ScheduleRequest(1, LocalDate.now(), "  在宅  "));
        assertThat(r.content()).isEqualTo("在宅");
    }

    @Test
    void create_バリデーションNGはValidationException() {
        assertThatThrownBy(() -> service.create(new ScheduleRequest(1, LocalDate.now(), "")))
                .isInstanceOf(ValidationException.class);
        verify(repo, times(0)).save(any());
    }

    @Test
    void update_存在しないIDはNotFound() {
        when(repo.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(999L,
                new ScheduleRequest(1, LocalDate.now(), "foo")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_正常() {
        Schedule s = new Schedule(1, LocalDate.of(2026, 4, 24), "在宅");
        setId(s, 5L);
        when(repo.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(s));

        ScheduleResponse r = service.update(5L,
                new ScheduleRequest(2, LocalDate.of(2026, 4, 25), "出社"));

        assertThat(r.id()).isEqualTo(5L);
        assertThat(r.memberId()).isEqualTo(2);
        assertThat(r.memberName()).isEqualTo("お母さん");
        assertThat(r.content()).isEqualTo("出社");
        assertThat(r.date()).isEqualTo(LocalDate.of(2026, 4, 25));
    }

    // U-01: delete() は論理削除し、deleteById を呼ばない
    @Test
    void delete_正常() {
        Schedule s = new Schedule(1, LocalDate.now(), "foo");
        setId(s, 10L);
        when(repo.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(s));
        service.delete(10L);
        assertThat(s.getDeletedAt()).isNotNull();
        verify(repo, times(0)).deleteById(any());
    }

    @Test
    void delete_存在しないIDはNotFound() {
        when(repo.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(NotFoundException.class);
    }

    // U-02: restore() は deletedAt を null にし、ScheduleResponse を返す
    @Test
    void restore_正常() {
        Schedule s = new Schedule(2, LocalDate.now(), "病院");
        setId(s, 20L);
        s.softDelete();
        when(repo.findByIdAndDeletedAtIsNotNull(20L)).thenReturn(Optional.of(s));

        ScheduleResponse r = service.restore(20L);

        assertThat(s.getDeletedAt()).isNull();
        assertThat(r.id()).isEqualTo(20L);
        assertThat(r.content()).isEqualTo("病院");
    }

    // U-03: purge() は物理削除する
    @Test
    void purge_正常() {
        Schedule s = new Schedule(1, LocalDate.now(), "foo");
        setId(s, 30L);
        s.softDelete();
        when(repo.findByIdAndDeletedAtIsNotNull(30L)).thenReturn(Optional.of(s));

        service.purge(30L);

        verify(repo).delete(s);
    }

    // U-04: 削除済みの予定を update しようとすると NotFound
    @Test
    void update_削除済みIDはNotFound() {
        when(repo.findByIdAndDeletedAtIsNull(55L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(55L,
                new ScheduleRequest(1, LocalDate.now(), "foo")))
                .isInstanceOf(NotFoundException.class);
    }

    // U-05: 削除されていない予定を restore しようとすると NotFound
    @Test
    void restore_未削除IDはNotFound() {
        when(repo.findByIdAndDeletedAtIsNotNull(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.restore(5L))
                .isInstanceOf(NotFoundException.class);
    }

    // U-06: 物理削除後の purge は NotFound
    @Test
    void purge_存在しないIDはNotFound() {
        when(repo.findByIdAndDeletedAtIsNotNull(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.purge(99L))
                .isInstanceOf(NotFoundException.class);
    }

    private static void setId(Schedule s, Long id) {
        try {
            Field f = Schedule.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(s, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
