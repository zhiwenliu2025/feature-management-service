package com.fms.console.shared.ui;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.VaadinSession;

/**
 * Global app + environment selector shown in the application navbar.
 */
public class GlobalContextBar extends HorizontalLayout {

  public static final String SESSION_APP_ID = "fms.context.appId";
  public static final String SESSION_ENVIRONMENT = "fms.context.environment";

  private final ComboBox<String> appSelector = new ComboBox<>("Application");
  private final ComboBox<String> environmentSelector = new ComboBox<>("Environment");

  public GlobalContextBar() {
    addClassName("fms-context-bar");
    setAlignItems(Alignment.CENTER);
    setSpacing(true);

    appSelector.setItems("checkout-service", "payments-api");
    appSelector.setWidth("220px");
    appSelector.setValue(resolveAppId());
    appSelector.addValueChangeListener(event -> {
      if (event.getValue() != null) {
        VaadinSession.getCurrent().setAttribute(SESSION_APP_ID, event.getValue());
      }
    });

    environmentSelector.setItems("dev", "staging", "prod");
    environmentSelector.setWidth("140px");
    environmentSelector.setValue(resolveEnvironment());
    environmentSelector.addValueChangeListener(event -> {
      if (event.getValue() != null) {
        VaadinSession.getCurrent().setAttribute(SESSION_ENVIRONMENT, event.getValue());
      }
    });

    add(new Span("App:"), appSelector, new Span("Env:"), environmentSelector);
  }

  public static String resolveAppId() {
    Object value = VaadinSession.getCurrent().getAttribute(SESSION_APP_ID);
    return value != null ? value.toString() : "checkout-service";
  }

  public static String resolveEnvironment() {
    Object value = VaadinSession.getCurrent().getAttribute(SESSION_ENVIRONMENT);
    return value != null ? value.toString() : "dev";
  }
}
