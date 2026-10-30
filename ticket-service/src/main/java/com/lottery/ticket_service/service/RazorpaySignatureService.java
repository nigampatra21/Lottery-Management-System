
package com.lottery.ticket_service.service;

import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpaySignatureService {

    private final String keySecret;

    public RazorpaySignatureService(
            @Value("${razorpay.key-secret}") String keySecret) {
        this.keySecret = keySecret;
    }


    public boolean verifyPaymentSignature(
            String orderId,
            String paymentId,
            String signature) {

        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);

            Utils.verifyPaymentSignature(attributes, keySecret);
            return true;

        } catch (Exception e) {
            return false;
        }
    }
}
