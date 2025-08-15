package org.example.lmsbackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.lmsbackend.dto.PaymentDTO;
import org.example.lmsbackend.dto.PaymentRequest;
import org.example.lmsbackend.dto.PaymentResponse;
import org.example.lmsbackend.dto.ZaloPayRequest;
import org.example.lmsbackend.security.CustomUserDetails;
import org.example.lmsbackend.service.PaymentService;
import org.example.lmsbackend.service.VNPayService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentRestController {

    private final PaymentService paymentService;
    private final VNPayService vnPayService;

    public PaymentRestController(PaymentService paymentService, VNPayService vnPayService) {
        this.paymentService = paymentService;
        this.vnPayService = vnPayService;
    }

    /**
     * Tạo payment cho khóa học có phí
     */
  @PostMapping("/create")
@PreAuthorize("hasAnyRole('student', 'instructor')")
public ResponseEntity<?> createPayment(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody Map<String, Object> requestBody,
        HttpServletRequest httpRequest
) {
    try {
        // 1. Lấy userId từ token
        Integer userId = userDetails.getUserId();

        // 2. Lấy courseId (chấp nhận "courseId" hoặc "course_id")
        Integer courseId = parseInteger(requestBody.getOrDefault("courseId", requestBody.get("course_id")));
        if (courseId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "courseId is required"));
        }

        // 3. Lấy amount (nếu có)
        BigDecimal amount = parseBigDecimal(requestBody.get("amount"));

        // 4. Lấy paymentMethod (nếu có)
        String paymentMethod = requestBody.get("paymentMethod") != null
                ? requestBody.get("paymentMethod").toString()
                : null;

        // 5. Tạo PaymentRequest
        PaymentRequest request = new PaymentRequest();
        request.setCourseId(courseId);
        request.setAmount(amount);
        request.setPaymentMethod(paymentMethod);

        // 6. Gọi service tạo thanh toán
        PaymentResponse response = paymentService.createPayment(userId, request, httpRequest);

        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Lỗi hệ thống: " + e.getMessage()));
    }
}

// =======================
// Hàm parse hỗ trợ
// =======================
private Integer parseInteger(Object value) {
    if (value == null) return null;
    if (value instanceof Integer) return (Integer) value;
    if (value instanceof Double) return ((Double) value).intValue();
    return Integer.valueOf(value.toString());
}

private BigDecimal parseBigDecimal(Object value) {
    if (value == null) return null;
    if (value instanceof Integer) return new BigDecimal((Integer) value);
    if (value instanceof Double) return BigDecimal.valueOf((Double) value);
    return new BigDecimal(value.toString());
}

    /**
     * Callback từ cổng thanh toán để xác nhận kết quả
     */
    @PostMapping("/callback")
    public ResponseEntity<?> paymentCallback(
            @RequestParam String transactionId,
            @RequestParam String status
    ) {
        try {
            PaymentResponse response = paymentService.confirmPayment(transactionId, status);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi xử lý callback: " + e.getMessage()));
        }
    }

    /**
     * Xử lý callback từ VNPay - Redirect về frontend
     */
    @GetMapping("/vnpay-callback")
    public ModelAndView vnpayCallback(HttpServletRequest request) {
        try {
            PaymentResponse response = paymentService.handleVNPayCallback(request);
            
            String transactionId = request.getParameter("vnp_TxnRef");
            String responseCode = request.getParameter("vnp_ResponseCode");
            String amount = request.getParameter("vnp_Amount");
            
            // Tạo URL redirect về frontend với query params
            StringBuilder redirectUrl = new StringBuilder("https://lms-frontend001-d43a1c85c11e.herokuapp.com/payment-success");
            redirectUrl.append("?vnp_ResponseCode=").append(responseCode);
            redirectUrl.append("&vnp_TxnRef=").append(transactionId);
            redirectUrl.append("&vnp_Amount=").append(amount);
            redirectUrl.append("&success=").append(response.isSuccess() ? "true" : "false");
            try {
                redirectUrl.append("&message=").append(java.net.URLEncoder.encode(response.getMessage(), "UTF-8"));
            } catch (Exception enc) {
                redirectUrl.append("&message=").append(response.getMessage());
            }
            
            return new ModelAndView("redirect:" + redirectUrl.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            // Redirect về frontend với error
            String errorUrl = "https://lms-frontend001-d43a1c85c11e.herokuapp.com/payment-success?success=false&message=Error";
            return new ModelAndView("redirect:" + errorUrl);
        }
    }

    /**
     * Lấy lịch sử thanh toán của user hiện tại
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('student', 'instructor', 'admin')")
    public ResponseEntity<?> getPaymentHistory(@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Integer userId = userDetails.getUserId();
            List<PaymentDTO> payments = paymentService.getPaymentHistory(userId);
            return ResponseEntity.ok(payments);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi khi lấy lịch sử thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Lấy tất cả thanh toán (chỉ dành cho admin)
     */
    @GetMapping("/admin/all-payments")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> getAllPayments(@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            List<PaymentDTO> payments = paymentService.getAllPayments();
            return ResponseEntity.ok(payments);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi khi lấy tất cả lịch sử thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách thanh toán của khóa học (cho instructor và admin)
     */
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('instructor', 'admin')")
    public ResponseEntity<?> getPaymentsByCourse(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            // TODO: Kiểm tra quyền - instructor chỉ được xem payment của khóa học mình dạy
            
            List<PaymentDTO> payments = paymentService.getPaymentsByCourse(courseId);
            return ResponseEntity.ok(payments);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi khi lấy danh sách thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra user đã thanh toán cho khóa học chưa
     */
    @GetMapping("/check/{courseId}")
    @PreAuthorize("hasAnyRole('student', 'instructor', 'admin')")
    public ResponseEntity<?> checkPaymentStatus(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Integer userId = userDetails.getUserId();
            boolean hasPaid = paymentService.hasCompletedPayment(userId, courseId);
            
            return ResponseEntity.ok(Map.of(
                    "courseId", courseId,
                    "userId", userId,
                    "hasPaid", hasPaid
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi khi kiểm tra trạng thái thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Endpoint test VNPay riêng
     */
    @PostMapping("/vnpay")
    @PreAuthorize("hasAnyRole('student', 'instructor', 'admin')")
    public ResponseEntity<?> createVNPayPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest httpRequest
    ) {
        try {
            // Check VNPay configuration first
            if (!vnPayService.isConfigValid()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "VNPay configuration is not properly set up",
                    "configStatus", vnPayService.getConfigStatus()
                ));
            }

            Integer userId = userDetails.getUserId();
            
            // Extract parameters from request body with safe type conversion
            Object amountObj = requestBody.get("amount");
            Object courseIdObj = requestBody.get("courseId");
            
            // Safe conversion for amount - preserve decimal precision
            BigDecimal amount;
            if (amountObj instanceof Integer) {
                amount = new BigDecimal((Integer) amountObj);
            } else if (amountObj instanceof Double) {
                amount = BigDecimal.valueOf((Double) amountObj);
            } else if (amountObj != null) {
                amount = new BigDecimal(amountObj.toString());
            } else {
                throw new IllegalArgumentException("Amount is required");
            }
            
            // Safe conversion for courseId  
            Integer courseId;
            if (courseIdObj instanceof Integer) {
                courseId = (Integer) courseIdObj;
            } else if (courseIdObj instanceof Double) {
                courseId = ((Double) courseIdObj).intValue();
            } else if (courseIdObj != null) {
                courseId = Integer.valueOf(courseIdObj.toString());
            } else {
                throw new IllegalArgumentException("CourseId is required");
            }
            
            // Create PaymentRequest for VNPay
            PaymentRequest request = new PaymentRequest();
            request.setCourseId(courseId);
            request.setAmount(amount);
            request.setPaymentMethod("vnpay");
            
            PaymentResponse response = paymentService.createPayment(userId, request, httpRequest);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "paymentUrl", response.getPaymentUrl(),
                    "message", "VNPay payment URL created successfully"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", response.getMessage()
                ));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false, 
                        "message", "Lỗi hệ thống: " + e.getMessage()
                    ));
        }
    }

    /**
     * VNPay payment callback API - Để frontend gọi và nhận JSON response
     */
    @GetMapping("/vnpay-payment-callback")
    public ResponseEntity<?> vnpayPaymentCallback(HttpServletRequest request) {
        try {
            String transactionId = request.getParameter("vnp_TxnRef");
            String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
            String vnp_TransactionStatus = request.getParameter("vnp_TransactionStatus");
            
            // 🚨 TEMP DEBUG
            System.out.println("=== Frontend Callback API Debug ===");
            System.out.println("Transaction ID: " + transactionId);
            System.out.println("vnp_ResponseCode: " + vnp_ResponseCode);
            System.out.println("vnp_TransactionStatus: " + vnp_TransactionStatus);
            
            int result = vnPayService.orderReturn(request);
            System.out.println("VNPayService.orderReturn() result: " + result);
            
            PaymentResponse paymentResponse;
            if (result == 1) {
                // Thanh toán thành công
                System.out.println("→ Processing SUCCESS payment via API");
                paymentResponse = paymentService.confirmPayment(transactionId, "success");
            } else if (result == 0) {
                // Thanh toán thất bại
                System.out.println("→ Processing FAILED payment via API");
                paymentResponse = paymentService.confirmPayment(transactionId, "failed");
            } else {
                // Lỗi signature
                System.out.println("→ Processing SIGNATURE ERROR via API");
                paymentResponse = new PaymentResponse(false, "Chữ ký không hợp lệ");
            }
            
            return ResponseEntity.ok(paymentResponse);
            
        } catch (Exception e) {
            System.err.println("❌ Frontend callback API error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi xử lý callback: " + e.getMessage()));
        }
    }

    /**
     * VNPay return callback endpoint - xử lý khi user quay về từ VNPay
     */
    @GetMapping("/vnpay-payment/return")
    public ResponseEntity<?> vnpayReturn(HttpServletRequest request, HttpServletResponse response) {
        try {
            String transactionId = request.getParameter("vnp_TxnRef");
            String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
            String vnp_TransactionStatus = request.getParameter("vnp_TransactionStatus");
            
            // 🚨 TEMP DEBUG
            System.out.println("=== PaymentRestController Return Debug ===");
            System.out.println("Transaction ID: " + transactionId);
            System.out.println("vnp_ResponseCode: " + vnp_ResponseCode);
            System.out.println("vnp_TransactionStatus: " + vnp_TransactionStatus);
            
            int result = vnPayService.orderReturn(request);
            System.out.println("VNPayService.orderReturn() result: " + result);
            
            String redirectUrl;
            if (result == 1) {
                // Thanh toán thành công - cập nhật DB và enroll user
                System.out.println("→ Processing SUCCESS payment");
                paymentService.confirmPayment(transactionId, "success");
                redirectUrl = "https://lms-frontend001-d43a1c85c11e.herokuapp.com/payment-success?status=success";
            } else if (result == 0) {
                // Thanh toán thất bại - cập nhật DB status = failed
                System.out.println("→ Processing FAILED payment");
                paymentService.confirmPayment(transactionId, "failed");
                redirectUrl = "https://lms-frontend001-d43a1c85c11e.herokuapp.com/payment-success?status=failed";
            } else {
                // Lỗi signature
                System.out.println("→ Processing SIGNATURE ERROR");
                redirectUrl = "https://lms-frontend001-d43a1c85c11e.herokuapp.com/payment-success?status=error";
            }
            
            System.out.println("Final redirect URL: " + redirectUrl);
            
            // Redirect về frontend
            response.sendRedirect(redirectUrl);
            return null;
        } catch (Exception e) {
            System.err.println("❌ Return callback error: " + e.getMessage());
            e.printStackTrace();
            try {
                response.sendRedirect("https://lms-frontend001-d43a1c85c11e.herokuapp.com/payment-success?status=error");
            } catch (Exception ex) {
                return ResponseEntity.status(500).body(Map.of("error", "Payment callback failed"));
            }
            return null;
        }
    }

    /**
     * Debug endpoint để kiểm tra VNPay config
     */
    @GetMapping("/vnpay-config")
    public ResponseEntity<?> getVNPayConfig() {
        try {
            Map<String, Object> configStatus = vnPayService.getConfigStatus();
            return ResponseEntity.ok(configStatus);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Test VNPay signature generation
     */
    @PostMapping("/vnpay-test-signature")
    public ResponseEntity<?> testVNPaySignature(
            @RequestBody Map<String, Object> testData,
            HttpServletRequest httpRequest
    ) {
        try {
            Integer amount = (Integer) testData.get("amount");
            String orderInfo = (String) testData.get("orderInfo");
            
            String paymentUrl = vnPayService.createOrder(httpRequest, amount, orderInfo, null);
            
            // Extract signature từ URL
            String signature = paymentUrl.substring(paymentUrl.lastIndexOf("vnp_SecureHash=") + 15);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "paymentUrl", paymentUrl,
                "signature", signature,
                "amount", amount,
                "orderInfo", orderInfo
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Tạo payment ZaloPay
     */
    @PostMapping("/zalopay")
    @PreAuthorize("hasAnyRole('student', 'instructor')")
    public ResponseEntity<?> createZaloPayPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ZaloPayRequest request
    ) {
        try {
            Integer userId = userDetails.getUserId();
            
            PaymentResponse response = paymentService.createZaloPayPayment(
                userId, 
                request.getCourseId(), 
                request.getAmount(), 
                request.getOrderInfo(),
                request.getDescription()
            );

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi tạo thanh toán ZaloPay: " + e.getMessage()));
        }
    }

    /**
     * Callback từ ZaloPay
     */
    @PostMapping("/zalopay-callback")
    public ResponseEntity<?> zaloPayCallback(@RequestBody Map<String, String> callbackData) {
        try {
            PaymentResponse response = paymentService.handleZaloPayCallback(callbackData);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(Map.of("return_code", 1, "return_message", "success"));
            } else {
                return ResponseEntity.ok(Map.of("return_code", -1, "return_message", response.getMessage()));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("return_code", -1, "return_message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Query trạng thái đơn hàng ZaloPay
     */
    @GetMapping("/zalopay-status/{appTransId}")
    @PreAuthorize("hasAnyRole('student', 'instructor')")
    public ResponseEntity<?> queryZaloPayOrder(@PathVariable String appTransId) {
        try {
            Map<String, Object> result = paymentService.queryZaloPayOrder(appTransId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy config ZaloPay (cho testing)
     */
    @GetMapping("/zalopay-config")
    public ResponseEntity<?> getZaloPayConfig() {
        try {
            Map<String, Object> config = paymentService.getZaloPayConfig();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
