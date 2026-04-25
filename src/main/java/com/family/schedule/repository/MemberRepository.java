package com.family.schedule.repository;

import com.family.schedule.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Integer> {
    List<Member> findAllByOrderByDisplayOrderAsc();
}
