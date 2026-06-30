package com.fms.console.admin.ui;

import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.PlaceholderView;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "admin/applications/:appId/keys", layout = MainLayout.class)
@AnonymousAllowed
public class ApiKeyListView extends PlaceholderView implements HasUrlParameter<String> {

  public ApiKeyListView() {
    super("API Keys", "API key lifecycle management for an application.");
  }

  @Override
  public void setParameter(BeforeEvent event, String appId) {
    getElement().setAttribute("data-app-id", appId);
  }
}
