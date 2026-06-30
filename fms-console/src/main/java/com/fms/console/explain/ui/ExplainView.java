package com.fms.console.explain.ui;

import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.PlaceholderView;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "explain", layout = MainLayout.class)
@AnonymousAllowed
public class ExplainView extends PlaceholderView {

  public ExplainView() {
    super("Explain Debugger", "Decision trace visualization for support queries.");
  }
}
