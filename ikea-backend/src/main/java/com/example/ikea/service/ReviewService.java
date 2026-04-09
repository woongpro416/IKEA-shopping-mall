package com.example.ikea.service;

import com.example.ikea.domain.Member;
import com.example.ikea.domain.Order;
import com.example.ikea.domain.OrderItem;
import com.example.ikea.domain.Product;
import com.example.ikea.domain.Review;
import com.example.ikea.dto.ReviewRequestDto;
import com.example.ikea.dto.ReviewResponseDto;
import com.example.ikea.repository.MemberRepository;
import com.example.ikea.repository.OrderItemRepository;
import com.example.ikea.repository.OrderRepository;
import com.example.ikea.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;

    public List<ReviewResponseDto> getReviewListByProduct(String productIdentifier) {
        Product product = productService.findProductEntityByIdentifier(productIdentifier);

        return reviewRepository.findByProduct_ProductIdOrderByCreatedAtDesc(product.getProductId())
                .stream()
                .map(ReviewResponseDto::new)
                .toList();
    }

    @Transactional
    public Long createReview(Long memberId, ReviewRequestDto dto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));

        Product product = productService.findProductEntityByRequest(dto.getProductId(), dto.getProductCode());

        Order order = null;
        if (dto.getOrderId() != null) {
            order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));

            if (order.getMember() == null || !order.getMember().getMemberId().equals(memberId)) {
                throw new AccessDeniedException("본인 주문에 대해서만 리뷰를 작성할 수 있습니다.");
            }

            boolean purchased = orderItemRepository
                    .existsByOrder_OrderIdAndProduct_ProductId(order.getOrderId(), product.getProductId());

            if (!purchased) {
                throw new IllegalArgumentException("해당 주문에 포함되지 않은 상품입니다.");
            }
        }

        Review review = Review.builder()
                .member(member)
                .product(product)
                .order(order)
                .content(dto.getContent())
                .rating(dto.getRating())
                .build();

        Review savedReview = reviewRepository.save(review);
        return savedReview.getReviewId();
    }

    @Transactional
    public void updateReview(Long reviewId, Long memberId, ReviewRequestDto dto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 리뷰입니다."));

        if (!review.getMember().getMemberId().equals(memberId)) {
            throw new AccessDeniedException("본인 리뷰만 수정할 수 있습니다.");
        }

        review.setContent(dto.getContent());
        review.setRating(dto.getRating());

        if (dto.getProductId() != null || (dto.getProductCode() != null && !dto.getProductCode().isBlank())) {
            Product product = productService.findProductEntityByRequest(dto.getProductId(), dto.getProductCode());
            review.setProduct(product);
        }

        if (dto.getOrderId() != null) {
            Order order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));

            if (order.getMember() == null || !order.getMember().getMemberId().equals(memberId)) {
                throw new AccessDeniedException("본인 주문에 대해서만 리뷰를 수정할 수 있습니다.");
            }

            boolean purchased = orderItemRepository
                    .existsByOrder_OrderIdAndProduct_ProductId(order.getOrderId(), review.getProduct().getProductId());

            if (!purchased) {
                throw new IllegalArgumentException("해당 주문에 포함되지 않은 상품입니다.");
            }

            review.setOrder(order);
        }
    }

    @Transactional
    public void deleteReview(Long reviewId, Long memberId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 리뷰입니다."));

        if (!review.getMember().getMemberId().equals(memberId)) {
            throw new AccessDeniedException("본인 리뷰만 삭제할 수 있습니다.");
        }

        reviewRepository.delete(review);
    }
}