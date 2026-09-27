package com.dmg.movieticketing.notification.application;

/**
 * Provider-neutral delivery port for booking lifecycle notifications.
 * Email, SMS, or other adapters can replace the local logging implementation.
 */
public interface NotificationSender {

    /** Delivers one channel-neutral notification or throws when the provider cannot accept it. */
    void send(NotificationMessage notification);
}
