package com.family.schedule.repository;

import com.family.schedule.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Integer> {
    List<Member> findAllByOrderByDisplayOrderAsc();
    boolean existsByName(String name);

    @Query("SELECT COALESCE(MAX(m.id), 0) FROM Member m")
    int findMaxId();

    @Query("SELECT COALESCE(MAX(m.displayOrder), 0) FROM Member m")
    int findMaxDisplayOrder();
}
