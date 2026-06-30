package com.fms.console.dashboard.ui;

import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.PlaceholderView;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "", layout = MainLayout.class)
@AnonymousAllowed
public class DashboardView extends PlaceholderView {

  public DashboardView() {
    super(
        "Dashboard",
        "KPI cards, recent audit events, and draft-dirty flags for the selected app and environment.");
  }
}
