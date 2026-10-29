package com.miniecommerce.order.example;

interface PaymentService {
    void pay(int amount);
}

class MomoPayment implements PaymentService {
    @Override
    public void pay(int amount) {
        System.out.println("Momo: " + amount);
    }
}

class VisaPayment implements PaymentService {
    @Override
    public void pay(int amount) {
        System.out.println("Visa: " + amount);
    }
}

public class Demo {
    public static void main(String[] args) {
        PaymentService p = args[0].equals("momo")
                ? new MomoPayment()
                : new VisaPayment();
        p.pay(100);
    }
}
