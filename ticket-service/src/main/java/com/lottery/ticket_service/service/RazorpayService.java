package com.lottery.ticket_service.service;

import com.lottery.ticket_service.exception.ServiceUnavailableException;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class RazorpayService {

    private final String keyId;
    private final String keySecret;
    private volatile RazorpayClient razorpayClient;

    public RazorpayService(
            @Value("${razorpay.key-id:}") String keyId,
            @Value("${razorpay.key-secret:}") String keySecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    public String getKeyId() {
        return keyId;
    }

    public String createOrder(BigDecimal amount, String receipt) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", toPaise(amount));
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receipt);
        try {
            Order order = getRazorpayClient().orders.create(orderRequest);
            return order.get("id");
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceUnavailableException("Unable to create order with Razorpay", e);
        }
    }

    public Payment fetchPayment(String paymentId) {
        try {
            return getRazorpayClient().payments.fetch(paymentId);
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceUnavailableException(
                    "Unable to fetch payment details from Razorpay", e);
        }
    }

    public boolean isPaymentCapturedForOrder(String paymentId, String expectedOrderId,
                                             BigDecimal expectedAmount) {
        Payment payment = fetchPayment(paymentId);

        String actualStatus = payment.get("status");
        String actualOrderId = payment.get("order_id");
        String actualCurrency = payment.get("currency");
        long actualAmountPaise = ((Number) payment.get("amount")).longValue();

        return "captured".equalsIgnoreCase(actualStatus)
                && expectedOrderId.equals(actualOrderId)
                && "INR".equalsIgnoreCase(actualCurrency)
                && actualAmountPaise == toPaise(expectedAmount);
    }

    /** Ids of the captured payments for an order. Used to recover unverified payments. */
    public List<String> findCapturedPaymentIds(String orderId) {
        try {
            List<Payment> payments = getRazorpayClient().orders.fetchPayments(orderId);
            List<String> ids = new ArrayList<>();
            for (Payment p : payments) {
                String status = p.get("status");
                if ("captured".equalsIgnoreCase(status)) {
                    String id = p.get("id");
                    ids.add(id);
                }
            }
            return ids;
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceUnavailableException(
                    "Unable to list payments for the order from Razorpay", e);
        }
    }

    /** Full refund. Safe to retry: a payment that is already refunded is not refunded twice. */
    public String refundPayment(String paymentId, BigDecimal amount) {
        long paise = toPaise(amount);
        try {
            Payment payment = getRazorpayClient().payments.fetch(paymentId);
            long alreadyRefunded = ((Number) payment.get("amount_refunded")).longValue();
            if (alreadyRefunded >= paise) {
                return "ALREADY-REFUNDED";
            }
            JSONObject request = new JSONObject();
            request.put("amount", paise);
            Refund refund = getRazorpayClient().payments.refund(paymentId, request);
            return refund.get("id");
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceUnavailableException("Unable to refund the payment with Razorpay", e);
        }
    }

    private long toPaise(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }

    private synchronized RazorpayClient getRazorpayClient() throws Exception {
        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new ServiceUnavailableException(
                    "Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
        if (razorpayClient == null) {
            razorpayClient = new RazorpayClient(keyId, keySecret);
        }
        return razorpayClient;
    }
}