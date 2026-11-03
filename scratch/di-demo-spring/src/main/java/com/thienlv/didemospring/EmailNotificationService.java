package com.thienlv.didemospring;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

//@Primary
@Service
public class EmailNotificationService implements NotificationService {
    @Override
    public void notify(String message) {
        System.out.println("[EMAIL] " + message);
    }
}
