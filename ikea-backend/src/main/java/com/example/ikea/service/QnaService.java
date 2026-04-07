package com.example.ikea.service;

import com.example.ikea.domain.Member;
import com.example.ikea.domain.Qna;
import com.example.ikea.dto.QnaRequestDto;
import com.example.ikea.dto.QnaResponseDto;
import com.example.ikea.repository.MemberRepository;
import com.example.ikea.repository.QnaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaService {

    private final QnaRepository qnaRepository;
    private final MemberRepository memberRepository;

    // 내 질문 목록
    public List<QnaResponseDto> getQnaList(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return qnaRepository.findByMemberIdAndLevelOrderByCreatedAtDesc(member.getMemberId(), 0)
                .stream()
                .map(QnaResponseDto::new)
                .collect(Collectors.toList());
    }

    // 특정 질문의 답변 목록
    public List<QnaResponseDto> getAnswerList(Long parentId) {
        return qnaRepository.findByParentIdAndLevelOrderByCreatedAtAsc(parentId, 1).stream()
                .map(QnaResponseDto::new)
                .collect(Collectors.toList());
    }

    // 질문 상세 조회
    public QnaResponseDto getQna(Long qnaId, String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Qna question = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (!question.getMemberId().equals(member.getMemberId())) {
            throw new AccessDeniedException("본인 문의만 조회할 수 있습니다.");
        }

        return new QnaResponseDto(question);
    }

    // 질문 등록 (일반회원)
    @Transactional
    public Long createQna(String loginId, QnaRequestDto dto) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Qna question = Qna.builder()
                .memberId(member.getMemberId())
                .title(dto.getTitle())
                .content(dto.getContent())
                .writer(member.getLoginId())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .level(0)
                .parentId(0L)
                .build();

        qnaRepository.save(question);
        question.setParentId(question.getQnaId());

        return question.getQnaId();
    }

    // 질문 수정 (일반회원)
    @Transactional
    public void updateQna(Long qnaId, String loginId, QnaRequestDto dto) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Qna question = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (question.getLevel() != 0) {
            throw new IllegalArgumentException("질문만 수정할 수 있습니다.");
        }

        if (!question.getMemberId().equals(member.getMemberId())) {
            throw new IllegalArgumentException("본인 글만 수정할 수 있습니다.");
        }

        question.setTitle(dto.getTitle());
        question.setContent(dto.getContent());
    }

    // 질문 삭제 (일반회원)
    @Transactional
    public void deleteQuestion(Long qnaId, String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Qna question = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (question.getLevel() != 0) {
            throw new IllegalArgumentException("질문만 삭제할 수 있습니다.");
        }

        if (!question.getMemberId().equals(member.getMemberId())) {
            throw new IllegalArgumentException("본인 글만 삭제할 수 있습니다.");
        }

        qnaRepository.deleteByParentIdAndLevel(question.getQnaId(), 1);
        qnaRepository.delete(question);
    }

    // ===================== 관리자 =====================

    // 전체 목록
    public List<QnaResponseDto> getAllQnaList() {
        return qnaRepository.findAllGrouped().stream()
                .map(QnaResponseDto::new)
                .collect(Collectors.toList());
    }

    // 제목 검색
    public List<QnaResponseDto> searchQna(String title) {
        return qnaRepository.findByTitle(title).stream()
                .map(QnaResponseDto::new)
                .collect(Collectors.toList());
    }

    // 상세 조회(관리자용)
    public QnaResponseDto getQnaForAdmin(Long qnaId) {
        return new QnaResponseDto(qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다.")));
    }

    // 답변 등록 (관리자)
    @Transactional
    public Long createAnswer(Long parentId, String loginId, QnaRequestDto dto) {
        Qna question = qnaRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 질문입니다."));

        if (question.getLevel() != 0) {
            throw new IllegalArgumentException("질문에만 답변할 수 있습니다.");
        }

        Qna answer = Qna.builder()
                .memberId(question.getMemberId())
                .title(dto.getTitle())
                .content(dto.getContent())
                .writer(loginId)
                .email(question.getEmail())
                .phoneNumber(question.getPhoneNumber())
                .level(1)
                .parentId(parentId)
                .build();

        return qnaRepository.save(answer).getQnaId();
    }

    // 답변 수정 (관리자)
    @Transactional
    public void updateAnswer(Long qnaId, QnaRequestDto dto) {
        Qna answer = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (answer.getLevel() != 1) {
            throw new IllegalArgumentException("답변만 수정할 수 있습니다.");
        }

        answer.setTitle(dto.getTitle());
        answer.setContent(dto.getContent());
    }

    // 답변 삭제 (관리자)
    @Transactional
    public void deleteAnswer(Long qnaId) {
        Qna answer = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (answer.getLevel() != 1) {
            throw new IllegalArgumentException("답변만 삭제할 수 있습니다.");
        }

        qnaRepository.delete(answer);
    }
}