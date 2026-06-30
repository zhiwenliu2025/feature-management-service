package com.fms.console.shared.ui;

import com.fms.console.shared.ui.components.FmsConfirmDialog;
import com.fms.console.shared.ui.components.UnsavedChangesGuard;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouteParameters;

import java.util.function.BooleanSupplier;

public interface SecuredView extends BeforeEnterObserver {

  BooleanSupplier accessCheck();

  default void beforeEnter(BeforeEnterEvent event) {
    if (!accessCheck().getAsBoolean()) {
      event.rerouteTo(ForbiddenView.class);
    }
    RouteParameters params = event.getRouteParameters();
    String appId = event.getLocation().getQueryParameters().getParameters().getOrDefault("appId", java.util.List.of()).stream()
        .findFirst()
        .orElse(GlobalContextBar.resolveAppId());
    String env = event.getLocation().getQueryParameters().getParameters().getOrDefault("env", java.util.List.of()).stream()
        .findFirst()
        .orElse(GlobalContextBar.resolveEnvironment());
    if (event.getLocation().getQueryParameters().getParameters().containsKey("appId")) {
      GlobalContextBar.setAppId(appId);
    }
    if (event.getLocation().getQueryParameters().getParameters().containsKey("env")) {
      GlobalContextBar.setEnvironment(env);
    }
  }
}
