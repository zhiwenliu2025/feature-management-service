package com.fms.console.flag.ui;

import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.PlaceholderView;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "flags/:flagKey/rules", layout = MainLayout.class)
@AnonymousAllowed
public class RuleEditorView extends PlaceholderView implements HasUrlParameter<String> {

  public RuleEditorView() {
    super("Rule Editor", "Environment-scoped rule list with drag-to-reorder priority.");
  }

  @Override
  public void setParameter(BeforeEvent event, String flagKey) {
    getElement().setAttribute("data-flag-key", flagKey);
  }
}
