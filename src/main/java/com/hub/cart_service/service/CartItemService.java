package com.hub.cart_service.service;

import com.hub.cart_service.grpc.CourseDetail;
import com.hub.cart_service.grpc.CourseListResponse;
import com.hub.cart_service.grpcclient.CourseGrpcClient;
import com.hub.cart_service.mapper.CartItemMapper;
import com.hub.cart_service.model.CartItem;
import com.hub.cart_service.model.dto.CartItemGetDto;
import com.hub.cart_service.model.dto.CartItemPostDto;
import com.hub.cart_service.repository.CartItemRepository;
import com.hub.cart_service.utils.Constants;
import com.hub.common_library.exception.DuplicatedException;
import com.hub.common_library.exception.InternalServerErrorException;
import com.hub.common_library.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartItemService {

    private final CartItemMapper cartItemMapper;
    private final CartItemRepository cartItemRepository;
    private final CourseGrpcClient courseGrpcClient;

    /**
     * Xem tất cả các item trong cart của user hiện tại
     */
    public List<CourseDetail> viewItemFromCartByUserId() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Long> courseIds = cartItemRepository.findCourseIdsByUserId(userId);

        if (courseIds.isEmpty()) {
            log.info("User {} has no courses in cart", userId);
            return List.of();
        }

        CourseListResponse response = courseGrpcClient.getCourseDetails(courseIds);
        return response.getCoursesList();
    }

    /**
     * Thêm item vào cart
     */
    @Transactional
    public CartItemGetDto addCartItem(CartItemPostDto cartItemPostDto) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        CartItem cartItem = performAddCartItem(cartItemPostDto, currentUserId);
        return cartItemMapper.toGetDto(cartItem);
    }

    private CartItem performAddCartItem(CartItemPostDto cartItemPostDto, String currentUserId) {
        try {
            Optional<CartItem> existingCartItem =
                    cartItemRepository.findByUserIdAndCourseId(currentUserId, cartItemPostDto.courseId());

            if (existingCartItem.isPresent()) {
                throw new DuplicatedException(Constants.ErrorCode.COURSE_ALREADY_IN_CART);
            }

            return createNewCartItem(cartItemPostDto, currentUserId);

        } catch (PessimisticLockingFailureException e) {
            log.error("CART_ITEM_SERVICE: Failed to acquire lock for adding cart item", e);
            throw new InternalServerErrorException(Constants.ErrorCode.ADD_CART_ITEM_FAILED);
        }
    }

    private CartItem createNewCartItem(CartItemPostDto cartItemPostDto, String currentUserId) {
        CartItem cartItem = new CartItem(currentUserId, cartItemPostDto.courseId());
        return cartItemRepository.save(cartItem);
    }

    /**
     * Xóa một course khỏi cart của user hiện tại
     */
    @Transactional
    public void removeCourseFromCart(Long courseId) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        CartItem cartItem = cartItemRepository.findByUserIdAndCourseId(currentUserId, courseId)
                .orElseThrow(() -> new NotFoundException(Constants.ErrorCode.COURSE_NOT_FOUND_IN_CART, courseId));

        cartItemRepository.delete(cartItem);
        log.info("Removed courseId {} from user {} cart", courseId, currentUserId);
    }

    /**
     * Xóa tất cả cart items chứa courseId (dùng khi course bị xóa)
     */
    @Transactional
    public void removeCourseFromCarts(Long courseId) {
        List<CartItem> cartItems = cartItemRepository.findAllByCourseId(courseId);

        if (cartItems.isEmpty()) {
            log.info("No cart items found for courseId {}", courseId);
            return;
        }

        cartItemRepository.deleteAll(cartItems);
        log.info("Removed {} cart items for deleted courseId {}", cartItems.size(), courseId);
    }

    /**
     * Lấy tất cả cart items theo courseId (nếu cần xử lý nhiều)
     */
    public List<CartItem> findAllByCourseId(Long courseId) {
        return cartItemRepository.findAllByCourseId(courseId);
    }
}
