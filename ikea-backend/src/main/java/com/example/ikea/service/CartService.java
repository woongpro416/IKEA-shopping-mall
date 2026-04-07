package com.example.ikea.service;

import com.example.ikea.domain.Cart;
import com.example.ikea.domain.CartItem;
import com.example.ikea.domain.Product;
import com.example.ikea.dto.CartItemResponseDto;
import com.example.ikea.dto.CartRequestDto;
import com.example.ikea.dto.GuestCartCreateResponseDto;
import com.example.ikea.repository.CartItemRepository;
import com.example.ikea.repository.CartRepository;
import com.example.ikea.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    //장바구니 조회
    @Transactional(readOnly = true)
    public List<CartItemResponseDto> getCartList(Long memberId) {
        Cart cart = cartRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장바구니입니다."));

        return cartItemRepository.findByCart_CartId(cart.getCartId()).stream()
                .map(CartItemResponseDto::new)
                .toList();
    }


    //장바구니 담기
    @Transactional
    public void addCart(Long memberId, CartRequestDto dto) {
        Cart cart = cartRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 존재하지 않습니다."));
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // quantity 하한 검증
        if (dto.getQuantity() < 1) {
            throw new IllegalArgumentException("수량은 최소 1개 이상이어야 합니다.");
        }

        //이미 담긴 상품이면 수량만 추가
        cartItemRepository.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), dto.getProductId())
                .ifPresentOrElse(
                        cp -> cp.setQuantity(cp.getQuantity() + dto.getQuantity()),
                        () -> cartItemRepository.save(CartItem.builder()
                                        .cart(cart)
                                        .product(product)
                                        .quantity(dto.getQuantity())
                                .build())
                );
    }

    //장바구니 상품 수량 수정
    @Transactional
    public void updateQuantity(Long cartItemId, int quantity, Long memberId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장바구니 입니다."));

        if (!cartItem.getCart().getMember().getMemberId().equals(memberId)) {
            throw new AccessDeniedException("본인 장바구니만 수정할 수 있습니다.");
        }

        if (quantity < 1) {
            throw new IllegalArgumentException("수량은 최소 1개 이상이어야 합니다.");
        }
        cartItem.setQuantity(quantity);
    }


    //장바구니 상품 단건 삭제
    @Transactional
    public void deleteCartItem(Long cartItemId, Long memberId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장바구니입니다."));
        if (!cartItem.getCart().getMember().getMemberId().equals(memberId)) {
            throw new AccessDeniedException("본인 장바구니만 삭제할 수 있습니다.");
        }
        cartItemRepository.deleteById(cartItemId);
    }

    //장바구니 전체 비우기
    @Transactional
    public void clearCart(Long memberId) {
        Cart cart = cartRepository.findByMember_MemberId(memberId).
                orElseThrow(() -> new IllegalArgumentException("장바구니가 존재하지 않습니다."));
        cartItemRepository.deleteByCart_CartId(cart.getCartId());
    }

    // =========== 비회원 전용 ==========

    //비회원 카트 생성/조회
    private Cart getOrCreateGuestCart(String guestCartKey) {
        if (guestCartKey == null || guestCartKey.isBlank()) {
            Cart newCart = Cart.builder()
                    .member(null)
                    .guestCartKey(java.util.UUID.randomUUID().toString())
                    .build();
            return cartRepository.save(newCart);
        }

        return cartRepository.findByGuestCartKey(guestCartKey)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder()
                                .member(null)
                                .guestCartKey(guestCartKey)
                                .build()
                ));
    }

    // 비회원 장바구니 목록 조회
    @Transactional(readOnly = true)
    public List<CartItemResponseDto> getGuestCartList(String guestCartKey) {
        Cart cart = cartRepository.findByGuestCartKey(guestCartKey)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 비회원 장바구니입니다."));

        return cartItemRepository.findByCart_CartId(cart.getCartId()).stream()
                .map(CartItemResponseDto::new)
                .toList();
    }

    // 비회원 장바구니 담기
    @Transactional
    public GuestCartCreateResponseDto addGuestCart(String guestCartKey, CartRequestDto dto) {
        Cart cart = getOrCreateGuestCart(guestCartKey);

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        if (dto.getQuantity() < 1) {
            throw new IllegalArgumentException("수량은 최소 1개 이상이어야 합니다.");
        }

        cartItemRepository.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), dto.getProductId())
                .ifPresentOrElse(
                        cartItem -> cartItem.setQuantity(cartItem.getQuantity() + dto.getQuantity()),
                        () -> cartItemRepository.save(CartItem.builder()
                                .cart(cart)
                                .product(product)
                                .quantity(dto.getQuantity())
                                .build())
                );

        return new GuestCartCreateResponseDto(cart.getGuestCartKey(), cart.getCartId());
    }

    // 비회원 장바구니 수량 수정
    @Transactional
    public void updateGuestQuantity(String guestCartKey, Long cartItemId, int quantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장바구니입니다."));

        Cart cart = cartItem.getCart();

        if (cart.getGuestCartKey() == null || !cart.getGuestCartKey().equals(guestCartKey)) {
            throw new AccessDeniedException("본인 비회원 장바구니만 수정할 수 있습니다.");
        }

        if (quantity < 1) {
            throw new IllegalArgumentException("수량은 최소 1개 이상이어야 합니다.");
        }

        cartItem.setQuantity(quantity);
    }

    // 비회원 장바구니 단건 삭제
    @Transactional
    public void deleteGuestCartItem(String guestCartKey, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장바구니입니다."));

        Cart cart = cartItem.getCart();

        if (cart.getGuestCartKey() == null || !cart.getGuestCartKey().equals(guestCartKey)) {
            throw new AccessDeniedException("본인 비회원 장바구니만 삭제할 수 있습니다.");
        }

        cartItemRepository.deleteById(cartItemId);
    }

    // 비회원 장바구니 전체 비우기
    @Transactional
    public void clearGuestCart(String guestCartKey) {
        Cart cart = cartRepository.findByGuestCartKey(guestCartKey)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 비회원 장바구니입니다."));

        cartItemRepository.deleteByCart_CartId(cart.getCartId());
    }

}
