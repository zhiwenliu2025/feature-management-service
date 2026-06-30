package com.fms.console.shared.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class PageHeader extends VerticalLayout {

  public PageHeader(String title, Component... actions) {
    this(title, null, actions);
  }

  public PageHeader(String title, String subtitle, Component... actions) {
    this(title, subtitle, null, actions);
  }

  public PageHeader(String title, String subtitle, Component[] badges, Component... actions) {
    addClassName("fms-page-header");
    setPadding(false);
    setSpacing(false);
    setWidthFull();

    H2 titleEl = new H2(title);
    titleEl.addClassName("fms-page-title");

    Div textBlock = new Div(titleEl);
    textBlock.addClassName("fms-page-header-text");

    if (subtitle != null && !subtitle.isBlank()) {
      Paragraph subtitleEl = new Paragraph(subtitle);
      subtitleEl.addClassName("fms-page-subtitle");
      textBlock.add(subtitleEl);
    }

    if (badges != null && badges.length > 0) {
      HorizontalLayout badgeRow = new HorizontalLayout();
      badgeRow.addClassName("fms-page-header-badges");
      badgeRow.setSpacing(true);
      badgeRow.setPadding(false);
      for (Component badge : badges) {
        badgeRow.add(badge);
      }
      textBlock.add(badgeRow);
    }

    if (actions != null && actions.length > 0) {
      HorizontalLayout actionRow = new HorizontalLayout();
      actionRow.addClassName("fms-page-header-actions");
      actionRow.setSpacing(true);
      actionRow.setPadding(false);
      for (Component action : actions) {
        actionRow.add(action);
      }

      HorizontalLayout row = new HorizontalLayout(textBlock, actionRow);
      row.setWidthFull();
      row.setAlignItems(Alignment.START);
      row.setJustifyContentMode(JustifyContentMode.BETWEEN);
      row.expand(textBlock);
      add(row);
    } else {
      add(textBlock);
    }
  }
}
