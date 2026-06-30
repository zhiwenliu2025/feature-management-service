package com.fms.console.release.ui;

import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.PlaceholderView;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "releases/:releaseId", layout = MainLayout.class)
@AnonymousAllowed
public class ReleaseDetailView extends PlaceholderView implements HasUrlParameter<String> {

  public ReleaseDetailView() {
    super("Release Detail", "Release metadata and linked flags.");
  }

  @Override
  public void setParameter(BeforeEvent event, String releaseId) {
    getElement().setAttribute("data-release-id", releaseId);
  }
}
