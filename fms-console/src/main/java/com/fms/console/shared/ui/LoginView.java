package com.fms.console.shared.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

  private final LoginOverlay login = new LoginOverlay();

  public LoginView() {
    setSizeFull();
    setAlignItems(Alignment.CENTER);
    setJustifyContentMode(JustifyContentMode.CENTER);
    add(new H1("FMS Admin Console"));
    add(new Paragraph("Sign in with your corporate account to continue."));
    login.setTitle("FMS");
    login.setDescription("Feature Management Service");
    login.setForgotPasswordButtonVisible(false);
    login.setAction("login");
    add(login);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
      login.setError(true);
    }
    login.setOpened(true);
  }
}
