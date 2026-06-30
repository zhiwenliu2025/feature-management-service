package com.fms.console.shared.ui;

import com.fms.console.shared.ui.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "forbidden", layout = MainLayout.class)
@AnonymousAllowed
public class ForbiddenView extends VerticalLayout {

  public ForbiddenView() {
    setPadding(true);
    add(new H2("Access denied"));
    add(new Paragraph("You do not have permission to view this page. Contact your administrator."));
  }
}
