package com.family.schedule.service;

import com.family.schedule.domain.Schedule;
import com.family.schedule.repository.ScheduleRepository;
import com.family.schedule.web.dto.ScheduleRequest;
import com.family.schedule.web.dto.ScheduleResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleService {

    private final ScheduleRepository repository;
    private final MemberService memberService;

    public ScheduleService(ScheduleRepository repository, MemberService memberService) {
        this.repository = repository;
        this.memberService = memberService;
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> findRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            Map<String, String> fields = new LinkedHashMap<>();
            if (from == null) fields.put("from", "必須です");
            if (to == null) fields.put("to", "必須です");
            throw new ValidationException("日付範囲が不正です", fields);
        }
        if (from.isAfter(to)) {
            throw new ValidationException("from は to 以前である必要があります", Map.of("from", "to 以前で指定してください"));
        }
        Map<Integer, String> names = memberService.nameById();
        return repository.findByDateBetweenAndDeletedAtIsNullOrderByDateAscMemberIdAscIdAsc(from, to).stream()
                .map(s -> ScheduleResponse.of(s, names.getOrDefault(s.getMemberId(), "")))
                .toList();
    }

    @Transactional
    public ScheduleResponse create(ScheduleRequest req) {
        Map<String, String> errors = ScheduleValidator.validate(req);
        if (!errors.isEmpty()) throw new ValidationException("入力に誤りがあります", errors);
        Schedule saved = repository.save(new Schedule(req.memberId(), req.date(), req.content().strip()));
        return ScheduleResponse.of(saved, memberService.nameById().getOrDefault(saved.getMemberId(), ""));
    }

    @Transactional
    public ScheduleResponse update(Long id, ScheduleRequest req) {
        Map<String, String> errors = ScheduleValidator.validate(req);
        if (!errors.isEmpty()) throw new ValidationException("入力に誤りがあります", errors);
        Schedule s = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("schedule not found: " + id));
        s.setMemberId(req.memberId());
        s.setDate(req.date());
        s.setContent(req.content().strip());
        return ScheduleResponse.of(s, memberService.nameById().getOrDefault(s.getMemberId(), ""));
    }

    @Transactional
    public void delete(Long id) {
        Schedule s = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("schedule not found: " + id));
        s.softDelete();
    }

    @Transactional
    public ScheduleResponse restore(Long id) {
        Schedule s = repository.findByIdAndDeletedAtIsNotNull(id)
                .orElseThrow(() -> new NotFoundException("schedule not found: " + id));
        s.restore();
        return ScheduleResponse.of(s, memberService.nameById().getOrDefault(s.getMemberId(), ""));
    }

    @Transactional
    public void purge(Long id) {
        Schedule s = repository.findByIdAndDeletedAtIsNotNull(id)
                .orElseThrow(() -> new NotFoundException("schedule not found: " + id));
        repository.delete(s);
    }
}
