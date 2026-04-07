package com.example.ikea.controller;

import com.example.ikea.dto.QnaRequestDto;
import com.example.ikea.dto.QnaResponseDto;
import com.example.ikea.service.MemberService;
import com.example.ikea.service.QnaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;



    //내 문의 목록
    @GetMapping
    public ResponseEntity<List<QnaResponseDto>> getQnaList(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(qnaService.getQnaList(userDetails.getUsername()));
    }

    // 질문 상세 + 답변 목록
    @GetMapping("/{qnaId}")
    public ResponseEntity<Map<String, Object>> getQna(
            @PathVariable Long qnaId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String loginId = userDetails.getUsername();

        QnaResponseDto question = qnaService.getQna(qnaId, loginId);
        List<QnaResponseDto> answers = qnaService.getAnswerList(question.getQnaId());

        Map<String, Object> response = new HashMap<>();
        response.put("question", question);
        response.put("answers", answers);

        return ResponseEntity.ok(response);
    }

    // 질문 등록
    @PostMapping
    public ResponseEntity<Long> createQna(@RequestBody @Valid QnaRequestDto dto,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(qnaService.createQna(userDetails.getUsername(), dto));
    }

    // 질문 수정
    @PutMapping("/{qnaId}")
    public ResponseEntity<Void> updateQna(@PathVariable Long qnaId,
                                          @RequestBody @Valid QnaRequestDto dto,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        qnaService.updateQna(qnaId, userDetails.getUsername(), dto);
        return ResponseEntity.ok().build();
    }

    // 질문 삭제
    @DeleteMapping("/{qnaId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long qnaId,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        qnaService.deleteQuestion(qnaId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}