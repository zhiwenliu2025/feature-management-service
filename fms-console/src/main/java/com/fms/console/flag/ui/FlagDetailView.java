package com.fms.console.flag.ui;

import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.PlaceholderView;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "flags/:flagKey", layout = MainLayout.class)
@AnonymousAllowed
public class FlagDetailView extends PlaceholderView implements HasUrlParameter<String> {

  public FlagDetailView() {
    super("Flag Detail", "Master-detail overview for a single feature flag.");
  }

  @Override
  public void setParameter(BeforeEvent event, String flagKey) {
    getElement().setAttribute("data-flag-key", flagKey);
  }
}
