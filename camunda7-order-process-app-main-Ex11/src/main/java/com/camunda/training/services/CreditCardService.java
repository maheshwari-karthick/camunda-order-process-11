package com.camunda.training.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class CreditCardService {

    private static final Logger LOG = LoggerFactory.getLogger(CreditCardService.class);

    public void chargeAmount(String cardNumber, String cvc, String expiryDate, Double amount) {
        LOG.info("charging card {} that expires on {} and has cvc {} with amount of {}",
                cardNumber, expiryDate, cvc, amount);

        if (validateExpiryDate(expiryDate) == false) {
            LOG.error("expiry date {} is invalid", expiryDate);
            throw new IllegalArgumentException("invalid expiry date");
        }


        LOG.info("payment completed");
    }

    public boolean validateExpiryDate(String expiryDate) {
        if (expiryDate.length() != 5) {
            return false;
        }
        try {
            int month = Integer.parseInt(expiryDate.substring(0, 2));
            int year = Integer.parseInt(expiryDate.substring(3, 5)) + 2000;
            LocalDate now = LocalDate.now();
            if (month < 1 || month > 12 || year < now.getYear()) {
                return false;
            }
            if (year > now.getYear() || month >= now.getMonthValue()) {
                return true;
            } else {
                return false;
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return false;
        }
    }

}
