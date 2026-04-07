package com.example.ikea.controller;

import com.example.ikea.domain.Qna;
import com.example.ikea.dto.QnaRequestDto;
import com.example.ikea.dto.QnaResponseDto;
import com.example.ikea.service.QnaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/qna")
public class AdminQnaController {

    private final QnaService qnaService;

    // 전체 목록
    @GetMapping
    public ResponseEntity<List<QnaResponseDto>> getAllQnaList() {
        return ResponseEntity.ok(qnaService.getAllQnaList());
    }

    // 제목 검색
    @GetMapping("/search")
    public ResponseEntity<List<QnaResponseDto>> searchQna(@RequestParam String title) {
        return ResponseEntity.ok(qnaService.searchQna(title));
    }

    // 답변 등록
    @PostMapping("/{parentId}/answer")
    public ResponseEntity<Long> createAnswer(@PathVariable Long parentId,
                                             @RequestBody @Valid QnaRequestDto dto,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                qnaService.createAnswer(parentId, userDetails.getUsername(), dto)
        );
    }

    // 답변 수정
    @PutMapping("/{qnaId}/answer")
    public ResponseEntity<Void> updateAnswer(@PathVariable Long qnaId,
                                             @RequestBody @Valid QnaRequestDto dto) {
        qnaService.updateAnswer(qnaId, dto);
        return ResponseEntity.ok().build();
    }

    // 답변 삭제
    @DeleteMapping("/{qnaId}/answer")
    public ResponseEntity<Void> deleteAnswer(@PathVariable Long qnaId) {
        qnaService.deleteAnswer(qnaId);
        return  ResponseEntity.ok().build();
    }
}
