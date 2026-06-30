package com.fms.console.shared.ui;

import com.fms.console.shared.ui.components.EmptyState;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "forbidden", layout = MainLayout.class)
@AnonymousAllowed
public class ForbiddenView extends VerticalLayout {

  public ForbiddenView() {
    setPadding(true);
    setSizeFull();
    setAlignItems(Alignment.CENTER);
    setJustifyContentMode(JustifyContentMode.CENTER);

    Div card = new Div();
    card.addClassName("fms-forbidden-card");
    card.getElement().getThemeList().add("surface");

    Icon icon = VaadinIcon.BAN.create();
    icon.addClassName("fms-empty-state-icon");
    icon.setSize("3rem");

    EmptyState state = new EmptyState(
        "Access denied",
        "You do not have permission to view this page. Contact your administrator if you need access.");
    state.addComponentAsFirst(icon);
    card.add(state);
    add(card);
  }
}
