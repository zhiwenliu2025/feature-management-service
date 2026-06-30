package com.fms.console.shared.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;

public class SectionCard extends Div {

  public SectionCard(String title, Component content) {
    addClassName("fms-section-card");
    getElement().getThemeList().add("surface");

    if (title != null && !title.isBlank()) {
      H3 heading = new H3(title);
      heading.addClassName("fms-section-title");
      add(heading);
    }
    add(content);
  }

  public SectionCard(Component content) {
    this(null, content);
  }
}
