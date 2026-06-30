package com.fms.console.shared.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;

public final class RouteLinks {

  private RouteLinks() {}

  public static RouterLink to(String text, Class<? extends Component> target, RouteParameters params) {
    RouterLink link = new RouterLink(text, target);
    link.setRoute(target, params);
    return link;
  }

  public static RouterLink flag(String text, String flagKey) {
    return to(text, com.fms.console.flag.ui.FlagDetailView.class,
        new RouteParameters("flagKey", flagKey));
  }

  public static RouterLink release(String text, String releaseId) {
    return to(text, com.fms.console.release.ui.ReleaseDetailView.class,
        new RouteParameters("releaseId", releaseId));
  }

  public static RouterLink apiKeys(String text, String appId) {
    return to(text, com.fms.console.admin.ui.ApiKeyListView.class,
        new RouteParameters("appId", appId));
  }

  public static RouteParameters flagParams(String flagKey) {
    return new RouteParameters("flagKey", flagKey);
  }
}
