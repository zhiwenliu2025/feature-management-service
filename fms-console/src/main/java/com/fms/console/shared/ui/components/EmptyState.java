package com.fms.console.shared.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class EmptyState extends VerticalLayout {

  public EmptyState(String title, String description) {
    this(title, description, null, null);
  }

  public EmptyState(String title, String description, String actionLabel, Runnable action) {
    addClassName("fms-empty-state");
    setAlignItems(Alignment.CENTER);
    setPadding(true);
    add(new H3(title));
  Paragraph body = new Paragraph(description);
    body.addClassName("fms-placeholder");
    add(body);
    if (actionLabel != null && action != null) {
      Button button = new Button(actionLabel, e -> action.run());
      add(button);
    }
  }
}
