package com.example.ikea.repository;

import com.example.ikea.domain.Qna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QnaRepository extends JpaRepository<Qna, Long> {

    // 관리자용 전체 목록 (그룹핑)
    @Query("SELECT q FROM Qna q ORDER BY q.parentId DESC, q.level ASC, q.createdAt ASC")
    List<Qna> findAllGrouped();

    // 관리자용 제목 검색
    @Query("""
            SELECT q FROM Qna q WHERE q.title LIKE %:title%
            ORDER BY q.parentId DESC, q.level ASC, q.createdAt ASC
            """)
    List<Qna> findByTitle(@Param("title") String title);

    // 특정 질문의 답변 목록
    List<Qna> findByParentIdOrderByLevelAscCreatedAtAsc(Long parentId);

    // 질문 삭제 시 답변도 삭제
    void deleteByParentId(Long parentId);

    //답변만 삭제
    void deleteByParentIdAndLevel(Long parentId, Integer level);

    // 답변 조회용
    List<Qna> findByParentIdAndLevelOrderByCreatedAtAsc(Long parentId, Integer level);

    // [추가] 회원 본인의 질문 목록만 조회 (level=0 이 질문, level=1 이 답변)
    // memberId 기준으로 본인 질문만 최신순 조회
    List<Qna> findByMemberIdAndLevelOrderByCreatedAtDesc(Long memberId, Integer level);

    List<Qna> findByMemberId(Long MemberId);
}
