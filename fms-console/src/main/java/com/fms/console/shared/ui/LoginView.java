package com.fms.console.shared.ui;

import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@AnonymousAllowed
public class LoginView extends LoginOverlay implements BeforeEnterObserver {

  public LoginView() {
    addClassName("fms-login-overlay");
    setTitle("FMS");
    setDescription("Feature Management Service — Admin Console");
    setForgotPasswordButtonVisible(false);
    setAction("login");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
      setError(true);
    }
    setOpened(true);
  }
}
