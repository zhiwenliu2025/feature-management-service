package com.fms.console.admin.ui;

import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.PlaceholderView;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "admin/applications", layout = MainLayout.class)
@AnonymousAllowed
public class ApplicationListView extends PlaceholderView {

  public ApplicationListView() {
    super("Applications", "Application onboarding and management.");
  }
}
