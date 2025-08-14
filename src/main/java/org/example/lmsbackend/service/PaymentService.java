package org.example.lmsbackend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.lmsbackend.dto.PaymentDTO;
import org.example.lmsbackend.dto.PaymentRequest;
import org.example.lmsbackend.dto.PaymentResponse;
import org.example.lmsbackend.model.Course;
import org.example.lmsbackend.model.Payment;
import org.example.lmsbackend.model.User;
import org.example.lmsbackend.repository.PaymentMapper;
import org.example.lmsbackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final CourseService courseService;
    private final EnrollmentsService enrollmentsService;
    private final VNPayService vnPayService;
    private final ZaloPayService zaloPayService;
    private final UserRepository userRepository;

    public PaymentService(PaymentMapper paymentMapper, CourseService courseService,
                          EnrollmentsService enrollmentsService, VNPayService vnPayService,
                          ZaloPayService zaloPayService, UserRepository userRepository) {
        this.paymentMapper = paymentMapper;
        this.courseService = courseService;
        this.enrollmentsService = enrollmentsService;
        this.vnPayService = vnPayService;
        this.zaloPayService = zaloPayService;
        this.userRepository = userRepository;
    }

    /**
     * Tạo payment cho khóa học có phí - với VNPay thật
     */
    @Transactional
    public PaymentResponse createPayment(Integer userId, PaymentRequest request, HttpServletRequest httpRequest) {
        try {
            // 1. Kiểm tra khóa học có tồn tại không
            Optional<Course> courseOpt = courseService.getCourseById(request.getCourseId());
            if (courseOpt.isEmpty()) {
                return new PaymentResponse(false, "Khóa học không tồn tại");
            }

            Course course = courseOpt.get();

            // 2. Kiểm tra khóa học có phí không
            if (course.getPrice().compareTo(BigDecimal.ZERO) == 0) {
                return new PaymentResponse(false, "Khóa học này miễn phí, không cần thanh toán");
            }

            // 3. Kiểm tra user đã đăng ký khóa học chưa
            if (enrollmentsService.isStudentEnrolled(userId, request.getCourseId())) {
                return new PaymentResponse(false, "Bạn đã đăng ký khóa học này rồi");
            }

            // 4. Kiểm tra đã thanh toán thành công chưa
            if (hasCompletedPayment(userId, request.getCourseId())) {
                return new PaymentResponse(false, "Bạn đã thanh toán cho khóa học này rồi");
            }

            // 5. Kiểm tra số tiền thanh toán có đúng không
            if (request.getAmount().compareTo(course.getPrice()) != 0) {
                return new PaymentResponse(false, "Số tiền thanh toán không chính xác. Yêu cầu: " + request.getAmount() + ", Giá khóa học: " + course.getPrice());
            }

            // 6. Tạo payment record
            Payment payment = new Payment();
            
            // Load actual user and course entities from database
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            payment.setUser(user);
            
            // Course đã được load ở trên, sử dụng lại
            payment.setCourse(course);
            
            payment.setAmount(request.getAmount());
            payment.setStatus(Payment.Status.pending);
            payment.setPaymentMethod(Payment.PaymentMethod.valueOf(request.getPaymentMethod()));
            payment.setTransactionId(generateTransactionId());

            int result = paymentMapper.insertPayment(payment);
            if (result > 0) {
                // 7. Tạo URL thanh toán với VNPay thật
                String paymentUrl = generateVNPayUrl(payment, httpRequest);
                
                return new PaymentResponse(true, "Tạo payment thành công", 
                                         payment.getPaymentId(), paymentUrl, payment.getTransactionId());
            } else {
                return new PaymentResponse(false, "Lỗi khi tạo payment");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new PaymentResponse(false, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    /**
     * Xác nhận thanh toán thành công (callback từ cổng thanh toán)
     */
    @Transactional
    public PaymentResponse confirmPayment(String transactionId, String status) {
        try {
            Optional<Payment> paymentOpt = paymentMapper.findPaymentByTransactionId(transactionId);
            if (paymentOpt.isEmpty()) {
                return new PaymentResponse(false, "Không tìm thấy giao dịch");
            }

            Payment payment = paymentOpt.get();

            if ("success".equals(status)) {
                // Cập nhật payment status thành completed
                int updated = paymentMapper.updatePaymentStatusByTransactionId(transactionId, "completed");
                
                if (updated > 0) {
                    // Tự động đăng ký khóa học cho user
                    boolean enrolled = enrollmentsService.enrollUserInCourse(
                        payment.getUser().getUserId(), 
                        payment.getCourse().getCourseId()
                    );
                    
                    if (enrolled) {
                        return new PaymentResponse(true, "Thanh toán thành công và đã đăng ký khóa học");
                    } else {
                        return new PaymentResponse(true, "Thanh toán thành công nhưng lỗi khi đăng ký khóa học");
                    }
                } else {
                    return new PaymentResponse(false, "Lỗi cập nhật trạng thái thanh toán");
                }
            } else {
                // Cập nhật payment status thành failed
                int updated = paymentMapper.updatePaymentStatusByTransactionId(transactionId, "failed");
                System.out.println("🔍 Debug Payment Failure Update Result: " + updated);
                return new PaymentResponse(false, "Thanh toán thất bại");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new PaymentResponse(false, "Lỗi khi xác nhận thanh toán: " + e.getMessage());
        }
    }

    /**
     * Lấy lịch sử thanh toán của user
     */
    public List<PaymentDTO> getPaymentHistory(Integer userId) {
        return paymentMapper.getPaymentsByUserId(userId);
    }

    /**
     * Lấy danh sách thanh toán của khóa học (cho instructor/admin)
     */
    public List<PaymentDTO> getPaymentsByCourse(Integer courseId) {
        return paymentMapper.getPaymentsByCourseId(courseId);
    }

    /**
     * Kiểm tra user đã thanh toán thành công cho khóa học chưa
     */
    public boolean hasCompletedPayment(Integer userId, Integer courseId) {
        return paymentMapper.countCompletedPayments(userId, courseId) > 0;
    }

    /**
     * Kiểm tra user có thể truy cập khóa học hay không (đã thanh toán hoặc khóa học miễn phí)
     */
    public boolean canAccessCourse(Integer userId, Integer courseId) {
        try {
            // Kiểm tra user đã enrolled chưa
            if (enrollmentsService.isStudentEnrolled(userId, courseId)) {
                return true;
            }
            
            // Kiểm tra khóa học có tồn tại không
            Optional<Course> courseOpt = courseService.getCourseById(courseId);
            if (courseOpt.isEmpty()) {
                return false;
            }
            
            Course course = courseOpt.get();
            
            // Nếu khóa học miễn phí thì cho phép truy cập
            if (course.getPrice().compareTo(BigDecimal.ZERO) == 0) {
                // Tự động enroll user vào khóa học miễn phí
                enrollmentsService.enrollUserInCourse(userId, courseId);
                return true;
            }
            
            // Kiểm tra đã thanh toán thành công chưa
            return hasCompletedPayment(userId, courseId);
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Tạo transaction ID duy nhất - VNPay compatible format
     * VNPay TxnRef chỉ cho phép tối đa 100 ký tự, nên tạo ID ngắn gọn hơn
     */
    private String generateTransactionId() {
        // Tạo ID ngắn gọn: timestamp + random 8 ký tự
        long timestamp = System.currentTimeMillis();
        String randomPart = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + "_" + randomPart; // Ví dụ: 1752614605850_d68a14ba
    }

    /**
     * Tạo URL thanh toán VNPay thật
     */
    private String generateVNPayUrl(Payment payment, HttpServletRequest httpRequest) {
        try {
            // Chỉ sử dụng VNPay cho vnpay payment method
            if (payment.getPaymentMethod() == Payment.PaymentMethod.vnpay) {
                // 🔧 FIX: Chuyển đổi amount chính xác cho VNPay (VND * 100)
                int amount = payment.getAmount().multiply(new BigDecimal("100")).intValue();
                String orderInfo = "Thanh toan khoa hoc " + payment.getCourse().getCourseId();
                // Sử dụng ReturnUrl từ VNPayConfig, không override
                String returnUrl = null; // VNPayService sẽ dùng config default
                
                // 🔧 FIX: Sử dụng Transaction ID từ database làm VNPay TxnRef để đồng bộ hóa
                return vnPayService.createOrderWithTxnRef(httpRequest, amount, orderInfo, returnUrl, payment.getTransactionId());
            } else {
                // Fallback cho các method khác (momo, zalopay, credit_card)
                return "https://lms-frontend001-d43a1c85c11e.herokuapp.com/payment-gateway?transaction_id=" + payment.getTransactionId() +
                       "&amount=" + payment.getAmount() + 
                       "&method=" + payment.getPaymentMethod() +
                       "&course_id=" + payment.getCourse().getCourseId() +
                       "&return_url=https://lms-frontend001-d43a1c85c11e.herokuapp.com/courses";
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to mock gateway if VNPay fails
            return "https://lms-frontend001-d43a1c85c11e.herokuapp.com/payment-gateway?transaction_id=" + payment.getTransactionId() +
                   "&amount=" + payment.getAmount() + 
                   "&method=" + payment.getPaymentMethod() +
                   "&course_id=" + payment.getCourse().getCourseId() +
                   "&return_url=https://lms-frontend001-d43a1c85c11e.herokuapp.com/courses";
        }
    }

    /**
     * Xử lý callback từ VNPay
     */
    public PaymentResponse handleVNPayCallback(HttpServletRequest request) {
        try {
            int result = vnPayService.orderReturn(request);
            String transactionId = request.getParameter("vnp_TxnRef");
            
            System.out.println("🔄 VNPay Callback - TxnRef: " + transactionId);
            System.out.println("🔄 VNPay Callback - Result: " + result);
            
            // 🔧 FIX: Lưu vnpayTxnRef vào database để đồng bộ hóa
            updatePaymentVnpayTxnRef(transactionId, transactionId);
            
            if (result == 1) {
                // Thanh toán thành công
                return confirmPayment(transactionId, "success");
            } else if (result == 0) {
                // Thanh toán thất bại
                return confirmPayment(transactionId, "failed");
            } else {
                // Chữ ký không hợp lệ
                return new PaymentResponse(false, "Chữ ký không hợp lệ");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new PaymentResponse(false, "Lỗi xử lý callback: " + e.getMessage());
        }
    }
    
    /**
     * Cập nhật vnpayTxnRef cho payment để đồng bộ hóa
     */
    private void updatePaymentVnpayTxnRef(String transactionId, String vnpayTxnRef) {
        try {
            paymentMapper.updatePaymentVnpayTxnRef(transactionId, vnpayTxnRef);
            System.out.println("✅ Updated vnpayTxnRef: " + vnpayTxnRef + " for transaction: " + transactionId);
        } catch (Exception e) {
            System.err.println("❌ Failed to update vnpayTxnRef: " + e.getMessage());
        }
    }

    /**
     * Tạo payment ZaloPay
     */
    @Transactional
    public PaymentResponse createZaloPayPayment(Integer userId, Integer courseId, Long amount, String orderInfo, String description) {
        try {
            // 1. Kiểm tra khóa học có tồn tại không
            Optional<Course> courseOpt = courseService.getCourseById(courseId);
            if (courseOpt.isEmpty()) {
                return new PaymentResponse(false, "Khóa học không tồn tại");
            }

            Course course = courseOpt.get();

            // 2. Kiểm tra user đã đăng ký khóa học chưa
            if (enrollmentsService.isUserEnrolledInCourse(userId, courseId)) {
                return new PaymentResponse(false, "Bạn đã đăng ký khóa học này rồi");
            }

            // 3. Kiểm tra user đã thanh toán cho khóa học chưa
            List<Payment> existingPayments = paymentMapper.findByCourseIdAndUserId(courseId, userId);
            for (Payment payment : existingPayments) {
                if ("completed".equals(payment.getStatus())) {
                    return new PaymentResponse(false, "Bạn đã thanh toán cho khóa học này rồi");
                }
            }

            // 4. Tạo payment record
            Payment payment = new Payment();
            
            // Lấy User và Course objects
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return new PaymentResponse(false, "Người dùng không tồn tại");
            }
            
            payment.setUser(userOpt.get());
            payment.setCourse(course);
            payment.setAmount(BigDecimal.valueOf(amount));
            payment.setPaymentMethod(Payment.PaymentMethod.zalopay);
            payment.setStatus(Payment.Status.pending);
            payment.setTransactionId(UUID.randomUUID().toString());

            paymentMapper.insertPayment(payment);

            // 5. Tạo order với ZaloPay
            Map<String, Object> zaloPayResponse = zaloPayService.createOrder(amount, description, orderInfo, courseId.toString());

            if (zaloPayResponse != null && "1".equals(zaloPayResponse.get("return_code").toString())) {
                // Thành công
                String orderUrl = (String) zaloPayResponse.get("order_url");
                String appTransId = (String) zaloPayResponse.get("app_trans_id");

                // Cập nhật payment với app_trans_id từ ZaloPay
                payment.setVnpayTxnRef(appTransId); // Sử dụng field này để lưu app_trans_id
                paymentMapper.updatePaymentVnpayTxnRef(payment.getTransactionId(), appTransId);

                return new PaymentResponse(true, "Tạo thanh toán ZaloPay thành công", 
                    payment.getPaymentId(), orderUrl, payment.getTransactionId(), course.getPrice().longValue(), courseId);
            } else {
                // Thất bại
                payment.setStatus(Payment.Status.failed);
                paymentMapper.updatePaymentStatus(payment);

                String errorMessage = zaloPayResponse != null ? 
                    (String) zaloPayResponse.get("return_message") : "Không thể tạo đơn hàng ZaloPay";
                
                return new PaymentResponse(false, "Lỗi tạo thanh toán ZaloPay: " + errorMessage);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new PaymentResponse(false, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    /**
     * Xử lý callback từ ZaloPay
     */
    @Transactional
    public PaymentResponse handleZaloPayCallback(Map<String, String> callbackData) {
        try {
            // 1. Xác thực callback
            if (!zaloPayService.verifyCallback(callbackData)) {
                return new PaymentResponse(false, "Xác thực callback không thành công");
            }

            // 2. Parse data từ callback
            String dataStr = callbackData.get("data");
            // Parse JSON để lấy thông tin
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = mapper.readValue(dataStr, Map.class);

            String appTransId = (String) data.get("app_trans_id");
            Long amount = ((Number) data.get("amount")).longValue();

            // 3. Tìm payment theo app_trans_id
            Payment payment = paymentMapper.findByVnpayTxnRef(appTransId);
            if (payment == null) {
                return new PaymentResponse(false, "Không tìm thấy giao dịch");
            }

            // 4. Cập nhật trạng thái payment
            payment.setStatus(Payment.Status.completed);
            paymentMapper.updatePaymentStatus(payment);

            // 5. Tự động enroll user vào khóa học
            try {
                enrollmentsService.enrollUserInCourse(payment.getUserId(), payment.getCourseId());
            } catch (Exception e) {
                // Log error but continue
            }

            return new PaymentResponse(true, "Thanh toán ZaloPay thành công", 
                payment.getPaymentId(), null, payment.getTransactionId(), amount, payment.getCourseId());

        } catch (Exception e) {
            e.printStackTrace();
            return new PaymentResponse(false, "Lỗi xử lý callback ZaloPay: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra trạng thái đơn hàng ZaloPay
     */
    public Map<String, Object> queryZaloPayOrder(String appTransId) {
        return zaloPayService.queryOrder(appTransId);
    }

    /**
     * Lấy config ZaloPay
     */
    public Map<String, Object> getZaloPayConfig() {
        return zaloPayService.getConfig();
    }
}

