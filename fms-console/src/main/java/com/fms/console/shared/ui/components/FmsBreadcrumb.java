package com.fms.console.shared.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.fms.console.shared.ui.RouteLinks;

public class FmsBreadcrumb extends HorizontalLayout {

  public FmsBreadcrumb() {
    addClassName("fms-breadcrumb");
    setSpacing(true);
    setAlignItems(Alignment.CENTER);
  }

  public FmsBreadcrumb segment(String label, Class<? extends Component> route) {
    RouterLink link = new RouterLink(label, route);
    add(link);
    add(separator());
    return this;
  }

  public FmsBreadcrumb segment(String label, Class<? extends Component> route, RouteParameters params) {
    add(RouteLinks.to(label, route, params));
    add(separator());
    return this;
  }

  public FmsBreadcrumb segment(String label, String href) {
    Anchor anchor = new Anchor(href, label);
    add(anchor);
    add(separator());
    return this;
  }

  public FmsBreadcrumb current(String label) {
    Span current = new Span(label);
    current.addClassName("fms-breadcrumb-current");
    add(current);
    return this;
  }

  private Span separator() {
    Span sep = new Span("/");
    sep.addClassName("fms-breadcrumb-sep");
    return sep;
  }
}
