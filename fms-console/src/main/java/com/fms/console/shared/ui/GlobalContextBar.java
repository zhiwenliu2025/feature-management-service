package com.fms.console.shared.ui;

import com.fms.console.admin.service.ApplicationUiService;
import com.fms.console.admin.service.EnvironmentUiService;
import com.fms.console.client.ApiClientExceptionHandler;
import com.fms.console.client.FmsUiException;
import com.fms.console.client.dto.ApplicationDtos.ApplicationDto;
import com.fms.console.client.dto.EnvironmentDtos.EnvironmentDto;
import com.fms.console.shared.ui.components.UnsavedChangesGuard;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import java.util.List;

@SpringComponent
@UIScope
public class GlobalContextBar extends HorizontalLayout {

  public static final String SESSION_APP_ID = "fms.context.appId";
  public static final String SESSION_ENVIRONMENT = "fms.context.environment";

  private final ComboBox<String> appSelector = new ComboBox<>("Application");
  private final ComboBox<String> environmentSelector = new ComboBox<>("Environment");

  public GlobalContextBar(ApplicationUiService applicationUiService, EnvironmentUiService environmentUiService) {
    addClassName("fms-context-bar");
    setAlignItems(Alignment.CENTER);
    setSpacing(true);

    Icon contextIcon = VaadinIcon.CLUSTER.create();
    contextIcon.setSize("var(--aura-icon-size-s)");

    appSelector.setWidth("12rem");
    environmentSelector.setWidth("9rem");
    appSelector.getElement().setAttribute("theme", "small");
    environmentSelector.getElement().setAttribute("theme", "small");

    try {
      List<String> apps = applicationUiService.list(50, null).data().stream()
          .map(ApplicationDto::slug)
          .toList();
      if (!apps.isEmpty()) {
        appSelector.setItems(apps);
        if (!apps.contains(resolveAppId())) {
          setAppId(apps.getFirst());
        }
      }
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
      appSelector.setItems("checkout-service");
    } catch (Exception ex) {
      appSelector.setItems("checkout-service");
    }

    try {
      List<String> envs = environmentUiService.list().stream().map(EnvironmentDto::name).toList();
      if (!envs.isEmpty()) {
        environmentSelector.setItems(envs);
        if (!envs.contains(resolveEnvironment())) {
          setEnvironment(envs.getFirst());
        }
      }
    } catch (Exception ex) {
      environmentSelector.setItems("dev", "staging", "prod");
    }

    appSelector.setValue(resolveAppId());
    environmentSelector.setValue(resolveEnvironment());

    appSelector.addValueChangeListener(event -> {
      if (!event.isFromClient() || event.getValue() == null) {
        return;
      }
      String newVal = event.getValue();
      String current = resolveAppId();
      if (newVal.equals(current)) {
        return;
      }
      appSelector.setValue(current);
      UnsavedChangesGuard.confirmIfDirty(() -> {
        setAppId(newVal);
        appSelector.setValue(newVal);
      });
    });

    environmentSelector.addValueChangeListener(event -> {
      if (!event.isFromClient() || event.getValue() == null) {
        return;
      }
      String newVal = event.getValue();
      String current = resolveEnvironment();
      if (newVal.equals(current)) {
        return;
      }
      environmentSelector.setValue(current);
      UnsavedChangesGuard.confirmIfDirty(() -> {
        setEnvironment(newVal);
        environmentSelector.setValue(newVal);
      });
    });

    add(contextIcon, appSelector, environmentSelector);
  }

  public static String resolveAppId() {
    Object value = VaadinSession.getCurrent().getAttribute(SESSION_APP_ID);
    return value != null ? value.toString() : "checkout-service";
  }

  public static String resolveEnvironment() {
    Object value = VaadinSession.getCurrent().getAttribute(SESSION_ENVIRONMENT);
    return value != null ? value.toString() : "dev";
  }

  public static void setAppId(String appId) {
    VaadinSession.getCurrent().setAttribute(SESSION_APP_ID, appId);
  }

  public static void setEnvironment(String environment) {
    VaadinSession.getCurrent().setAttribute(SESSION_ENVIRONMENT, environment);
  }
}
