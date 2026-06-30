package com.fms.console.shared.ui.components;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

public final class FmsNotification {

  private FmsNotification() {}

  public static void success(String message) {
    Notification.show(message, 3000, Notification.Position.BOTTOM_START)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  public static void error(String message) {
    Notification.show(message, 0, Notification.Position.TOP_CENTER)
        .addThemeVariants(NotificationVariant.LUMO_ERROR);
  }

  public static void info(String message) {
    Notification.show(message, 4000, Notification.Position.BOTTOM_START);
  }
}
