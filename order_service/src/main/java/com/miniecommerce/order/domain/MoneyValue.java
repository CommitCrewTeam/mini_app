package com.miniecommerce.order.domain;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;

import java.util.Objects;

public final class MoneyValue {

    public static final MoneyValue ZERO = new MoneyValue(0L);

    private final long amount;

    public MoneyValue(long amount) {
        if (amount < 0) {
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "Money must not be negative: " + amount);
        }
        this.amount = amount;
    }

    public static MoneyValue of(long amount) {
        return new MoneyValue(amount);
    }

    public long getAmount() {
        return amount;
    }

    public MoneyValue add(MoneyValue other) {
        return MoneyValue.of(this.amount + other.amount);
    }

    public MoneyValue multiply(int times) {
        return MoneyValue.of(this.amount * times);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MoneyValue moneyValue)) {
            return false;
        }
        return amount == moneyValue.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return "MoneyValue{" + "amount=" + amount + '}';
    }
}