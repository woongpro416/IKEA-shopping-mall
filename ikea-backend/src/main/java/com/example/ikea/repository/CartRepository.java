package com.example.ikea.repository;

import com.example.ikea.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    //회원 장바구니 조회 (회원당 1개)
    Optional<Cart> findByMember_MemberId(Long memberId);

    //비회원전용 장바구니 키 조회
    Optional<Cart> findByGuestCartKey(String guestCartKey);

}
