package com.fms.console.shared.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public abstract class PlaceholderView extends VerticalLayout {

  protected PlaceholderView(String title, String description) {
    addClassName("fms-placeholder-view");
    setPadding(true);
    setSpacing(true);

    H2 heading = new H2(title);
    heading.addClassName("fms-page-title");
    Paragraph body = new Paragraph(description);
    body.addClassName("fms-placeholder");

    add(heading, body);
  }
}
