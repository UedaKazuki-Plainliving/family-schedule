package com.family.schedule.service;

import com.family.schedule.domain.Member;
import com.family.schedule.repository.MemberRepository;
import com.family.schedule.web.dto.MemberRequest;
import com.family.schedule.web.dto.MemberResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MemberService {

    static final int MAX_MEMBERS = 10;

    private final MemberRepository repository;

    public MemberService(MemberRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> list() {
        return repository.findAllByOrderByDisplayOrderAsc().stream()
                .map(MemberResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Integer, String> nameById() {
        return repository.findAll().stream()
                .collect(Collectors.toMap(Member::getId, Member::getName));
    }

    @Transactional
    public MemberResponse create(MemberRequest req) {
        String name = req.name() == null ? "" : req.name().trim();
        if (name.isBlank()) {
            throw new ValidationException("名前を入力してください", Map.of("name", "名前を入力してください"));
        }
        if (name.length() > 20) {
            throw new ValidationException("名前は20文字以内で入力してください", Map.of("name", "名前は20文字以内で入力してください"));
        }
        if (repository.count() >= MAX_MEMBERS) {
            throw new ValidationException("メンバーは最大" + MAX_MEMBERS + "名までです", Map.of("name", "メンバーは最大" + MAX_MEMBERS + "名までです"));
        }
        if (repository.existsByName(name)) {
            throw new ValidationException("同じ名前のメンバーが既に存在します", Map.of("name", "同じ名前のメンバーが既に存在します"));
        }
        int nextId = repository.findMaxId() + 1;
        int nextOrder = repository.findMaxDisplayOrder() + 1;
        Member m = new Member(nextId, name, nextOrder);
        return MemberResponse.of(repository.save(m));
    }

    @Transactional
    public MemberResponse rename(Integer id, MemberRequest req) {
        Member m = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("メンバーが見つかりません: " + id));
        String name = req.name() == null ? "" : req.name().trim();
        if (name.isBlank()) {
            throw new ValidationException("名前を入力してください", Map.of("name", "名前を入力してください"));
        }
        if (name.length() > 20) {
            throw new ValidationException("名前は20文字以内で入力してください", Map.of("name", "名前は20文字以内で入力してください"));
        }
        if (!m.getName().equals(name) && repository.existsByName(name)) {
            throw new ValidationException("同じ名前のメンバーが既に存在します", Map.of("name", "同じ名前のメンバーが既に存在します"));
        }
        m.setName(name);
        return MemberResponse.of(m);
    }

    @Transactional
    public void delete(Integer id) {
        Member m = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("メンバーが見つかりません: " + id));
        repository.delete(m);
    }
}
