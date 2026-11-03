package com.thienlv.didemospring;

import org.springframework.stereotype.Service;

@Service
public class SmsNotificationService  implements NotificationService {
    @Override
    public void notify(String message) {
        System.out.println("[SMS] " + message);
    }
}
