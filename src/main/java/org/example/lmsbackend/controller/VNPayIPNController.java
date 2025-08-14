package org.example.lmsbackend.controller;

import org.example.lmsbackend.service.VNPayService;
import org.example.lmsbackend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vnpay")
@CrossOrigin(origins = "*")
public class VNPayIPNController {

    @Autowired
    private VNPayService vnPayService;
    
    @Autowired
    private PaymentService paymentService;

    /**
     * IPN URL - VNPay gọi về để thông báo kết quả thanh toán
     * Method: GET (theo docs VNPay)
     */
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> handleIPN(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();
        
        try {
            System.out.println("🔔 VNPay IPN received");
            
            // Log tất cả parameters
            request.getParameterMap().forEach((key, values) -> {
                if (key.startsWith("vnp_")) {
                    System.out.println(key + ": " + values[0]);
                }
            });
            
            // Lấy các tham số quan trọng
            String vnp_TxnRef = request.getParameter("vnp_TxnRef");
            String vnp_TransactionNo = request.getParameter("vnp_TransactionNo");
            String vnp_Amount = request.getParameter("vnp_Amount");
            String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
            String vnp_TransactionStatus = request.getParameter("vnp_TransactionStatus");
            String vnp_BankCode = request.getParameter("vnp_BankCode");
            String vnp_PayDate = request.getParameter("vnp_PayDate");
            
            System.out.println("🔍 IPN Key Info:");
            System.out.println("TxnRef: " + vnp_TxnRef);
            System.out.println("TransactionNo: " + vnp_TransactionNo);
            System.out.println("Amount: " + vnp_Amount);
            System.out.println("ResponseCode: " + vnp_ResponseCode);
            System.out.println("TransactionStatus: " + vnp_TransactionStatus);
            
            // 1. Kiểm tra chữ ký
            int signatureResult = vnPayService.orderReturn(request);
            
            if (signatureResult != 1) {
                System.out.println("❌ IPN: Invalid signature");
                response.put("RspCode", "97");
                response.put("Message", "Invalid signature");
                return ResponseEntity.ok(response);
            }
            
            // 2. Tìm transaction trong database
            boolean transactionExists = paymentService.checkTransactionExists(vnp_TxnRef);
            
            if (!transactionExists) {
                System.out.println("❌ IPN: Transaction not found - " + vnp_TxnRef);
                response.put("RspCode", "01");
                response.put("Message", "Order not found");
                return ResponseEntity.ok(response);
            }
            
            // 3. Kiểm tra transaction đã được xử lý chưa
            boolean alreadyProcessed = paymentService.isTransactionProcessed(vnp_TxnRef);
            
            if (alreadyProcessed) {
                System.out.println("✅ IPN: Transaction already processed - " + vnp_TxnRef);
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return ResponseEntity.ok(response);
            }
            
            // 4. Kiểm tra amount
            boolean amountValid = paymentService.validateTransactionAmount(vnp_TxnRef, vnp_Amount);
            
            if (!amountValid) {
                System.out.println("❌ IPN: Invalid amount for transaction - " + vnp_TxnRef);
                response.put("RspCode", "04");
                response.put("Message", "Invalid amount");
                return ResponseEntity.ok(response);
            }
            
            // 5. Cập nhật kết quả thanh toán
            boolean updateSuccess = false;
            
            if ("00".equals(vnp_ResponseCode) && "00".equals(vnp_TransactionStatus)) {
                // Thanh toán thành công
                updateSuccess = paymentService.updatePaymentSuccess(
                    vnp_TxnRef, 
                    vnp_TransactionNo, 
                    vnp_BankCode, 
                    vnp_PayDate
                );
                System.out.println("✅ IPN: Payment SUCCESS for " + vnp_TxnRef);
            } else {
                // Thanh toán thất bại
                updateSuccess = paymentService.updatePaymentFailed(
                    vnp_TxnRef, 
                    vnp_ResponseCode, 
                    vnp_TransactionStatus
                );
                System.out.println("❌ IPN: Payment FAILED for " + vnp_TxnRef + " - Code: " + vnp_ResponseCode);
            }
            
            if (updateSuccess) {
                response.put("RspCode", "00");
                response.put("Message", "Confirm Success");
                System.out.println("✅ IPN: Database updated successfully");
            } else {
                response.put("RspCode", "99");
                response.put("Message", "Database update failed");
                System.out.println("❌ IPN: Database update failed");
            }
            
        } catch (Exception e) {
            System.err.println("❌ IPN Error: " + e.getMessage());
            e.printStackTrace();
            
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
        }
        
        System.out.println("🔔 IPN Response: " + response);
        return ResponseEntity.ok(response);
    }
}
