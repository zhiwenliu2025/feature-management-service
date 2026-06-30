package com.fms.console.admin.ui;

import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.PlaceholderView;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "admin/environments", layout = MainLayout.class)
@AnonymousAllowed
public class EnvironmentListView extends PlaceholderView {

  public EnvironmentListView() {
    super("Environments", "Environment configuration per application.");
  }
}
