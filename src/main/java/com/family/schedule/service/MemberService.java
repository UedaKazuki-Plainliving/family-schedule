package com.family.schedule.service;

import com.family.schedule.domain.Member;
import com.family.schedule.repository.MemberRepository;
import com.family.schedule.web.dto.MemberResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MemberService {

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
}
