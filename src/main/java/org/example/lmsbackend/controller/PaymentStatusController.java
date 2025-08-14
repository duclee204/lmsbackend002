package org.example.lmsbackend.controller;

import org.example.lmsbackend.model.Payment;
import org.example.lmsbackend.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment-status")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentStatusController {

    @Autowired
    private PaymentRepository paymentRepository;

    // Kiểm tra theo Transaction Reference
    @GetMapping("/check/{txnRef}")
    public ResponseEntity<Map<String, Object>> checkPaymentByTxnRef(@PathVariable String txnRef) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<Payment> payment = paymentRepository.findByTransactionId(txnRef);
            
            if (payment.isPresent()) {
                Payment p = payment.get();
                response.put("found", true);
                response.put("payment", createPaymentInfo(p));
                response.put("message", "Tìm thấy giao dịch");
            } else {
                response.put("found", false);
                response.put("message", "Không tìm thấy giao dịch với mã: " + txnRef);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", true);
            response.put("message", "Lỗi khi kiểm tra: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Kiểm tra tất cả payment của một user
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> checkPaymentsByUser(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
            
            response.put("found", !payments.isEmpty());
            response.put("count", payments.size());
            response.put("payments", payments.stream().map(this::createPaymentInfo).toList());
            response.put("message", "Tìm thấy " + payments.size() + " giao dịch cho user " + userId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", true);
            response.put("message", "Lỗi khi kiểm tra: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Kiểm tra payment cho một course cụ thể
    @GetMapping("/course/{courseId}/user/{userId}")
    public ResponseEntity<Map<String, Object>> checkPaymentForCourse(
            @PathVariable Long courseId, 
            @PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Payment> payments = paymentRepository.findByCourseIdAndUserIdOrderByCreatedAtDesc(courseId, userId);
            
            // Tìm payment thành công gần nhất
            Optional<Payment> successPayment = payments.stream()
                    .filter(p -> Payment.Status.completed.equals(p.getStatus()))
                    .findFirst();
            
            response.put("found", successPayment.isPresent());
            response.put("allPayments", payments.stream().map(this::createPaymentInfo).toList());
            
            if (successPayment.isPresent()) {
                response.put("successPayment", createPaymentInfo(successPayment.get()));
                response.put("message", "User đã thanh toán thành công cho course này");
                response.put("isPaid", true);
            } else {
                response.put("message", "User chưa thanh toán thành công cho course này");
                response.put("isPaid", false);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", true);
            response.put("message", "Lỗi khi kiểm tra: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Kiểm tra tất cả payment gần đây
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentPayments(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Payment> payments = paymentRepository.findTop10ByOrderByCreatedAtDesc();
            
            response.put("count", payments.size());
            response.put("payments", payments.stream().map(this::createPaymentInfo).toList());
            response.put("message", "Danh sách " + payments.size() + " giao dịch gần nhất");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", true);
            response.put("message", "Lỗi khi lấy danh sách: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Manual fix payment
    @PostMapping("/fix/{paymentId}")
    public ResponseEntity<Map<String, Object>> fixPayment(
            @PathVariable Integer paymentId,
            @RequestParam String vnpayTxnRef,
            @RequestParam String status) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
            
            if (paymentOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không tìm thấy payment với ID: " + paymentId);
                return ResponseEntity.status(404).body(response);
            }
            
            Payment payment = paymentOpt.get();
            String oldTransactionId = payment.getTransactionId();
            Payment.Status oldStatus = payment.getStatus();
            
            // Update transaction ID và status
            payment.setTransactionId(vnpayTxnRef);
            
            try {
                Payment.Status newStatus = Payment.Status.valueOf(status);
                payment.setStatus(newStatus);
                
                if (newStatus == Payment.Status.completed && payment.getPaidAt() == null) {
                    payment.setPaidAt(Instant.now());
                }
            } catch (IllegalArgumentException e) {
                response.put("success", false);
                response.put("message", "Status không hợp lệ: " + status);
                return ResponseEntity.status(400).body(response);
            }
            
            payment.setUpdatedAt(Instant.now());
            
            // Save changes
            Payment savedPayment = paymentRepository.save(payment);
            
            response.put("success", true);
            response.put("message", "Payment đã được cập nhật thành công");
            response.put("paymentId", paymentId);
            response.put("oldTransactionId", oldTransactionId);
            response.put("newTransactionId", vnpayTxnRef);
            response.put("oldStatus", oldStatus);
            response.put("newStatus", savedPayment.getStatus());
            response.put("updatedAt", savedPayment.getUpdatedAt());
            response.put("payment", createPaymentInfo(savedPayment));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật payment: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Lấy thanh toán mới nhất
    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> getLatestPayment() {
        try {
            List<Payment> payments = paymentRepository.findAll();
            if (payments.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Không có thanh toán nào");
                return ResponseEntity.ok(response);
            }
            
            Payment latestPayment = payments.stream()
                .max((p1, p2) -> p1.getCreatedAt().compareTo(p2.getCreatedAt()))
                .orElse(null);
                
            return ResponseEntity.ok(createPaymentInfo(latestPayment));
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", true);
            response.put("message", "Lỗi khi lấy thanh toán: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Lấy tất cả thanh toán pending
    @GetMapping("/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingPayments() {
        try {
            List<Payment> pendingPayments = paymentRepository.findByStatus("pending");
            List<Map<String, Object>> response = pendingPayments.stream()
                .map(this::createPaymentInfo)
                .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    private Map<String, Object> createPaymentInfo(Payment payment) {
        Map<String, Object> info = new HashMap<>();
        info.put("paymentId", payment.getPaymentId());
        info.put("transactionId", payment.getTransactionId());
        info.put("vnpayTxnRef", payment.getVnpayTxnRef()); // Thêm vnpayTxnRef
        info.put("userId", payment.getUser() != null ? payment.getUser().getUserId() : null);
        info.put("courseId", payment.getCourse() != null ? payment.getCourse().getCourseId() : null);
        info.put("amount", payment.getAmount());
        info.put("status", payment.getStatus());
        info.put("paymentMethod", payment.getPaymentMethod());
        info.put("paymentGatewayResponse", payment.getPaymentGatewayResponse());
        info.put("createdAt", payment.getCreatedAt());
        info.put("updatedAt", payment.getUpdatedAt());
        info.put("paidAt", payment.getPaidAt());
        
        // Thêm thông tin trạng thái dễ hiểu
        String statusText = switch (payment.getStatus()) {
            case completed -> "✅ Thành công";
            case pending -> "⏳ Đang xử lý";
            case failed -> "❌ Thất bại";
            case refunded -> "💰 Đã hoàn tiền";
            default -> "❓ Không xác định";
        };
        info.put("statusText", statusText);
        
        return info;
    }
}
