package com.fms.console.client;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

public final class ApiClientExceptionHandler {

  private ApiClientExceptionHandler() {}

  public static void handle(FmsUiException ex) {
    Notification.show(ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
        .addThemeVariants(NotificationVariant.ERROR);
  }

  public static void handle(Exception ex) {
    Notification.show("An unexpected error occurred. Please try again.", 5000, Notification.Position.TOP_CENTER)
        .addThemeVariants(NotificationVariant.ERROR);
  }
}
