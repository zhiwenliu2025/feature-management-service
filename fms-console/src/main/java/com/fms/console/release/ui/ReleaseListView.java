package com.fms.console.release.ui;

import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.PlaceholderView;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "releases", layout = MainLayout.class)
@AnonymousAllowed
public class ReleaseListView extends PlaceholderView {

  public ReleaseListView() {
    super("Releases", "Release list linked to feature flags.");
  }
}
