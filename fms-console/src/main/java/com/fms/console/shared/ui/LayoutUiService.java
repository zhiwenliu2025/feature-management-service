package com.fms.console.shared.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import java.util.function.Consumer;

@SpringComponent
@UIScope
public class LayoutUiService {

  private Consumer<Component> breadcrumbSetter;
  private Consumer<String> bannerSetter;
  private Runnable bannerClearer;

  public void register(
      Consumer<Component> breadcrumbSetter,
      Consumer<String> bannerSetter,
      Runnable bannerClearer) {
    this.breadcrumbSetter = breadcrumbSetter;
    this.bannerSetter = bannerSetter;
    this.bannerClearer = bannerClearer;
  }

  public void setBreadcrumb(Component breadcrumb) {
    if (breadcrumbSetter != null) {
      breadcrumbSetter.accept(breadcrumb);
    }
  }

  public void showKillSwitchBanner(String message) {
    if (bannerSetter != null) {
      bannerSetter.accept(message);
    }
  }

  public void clearBanner() {
    if (bannerClearer != null) {
      bannerClearer.run();
    }
  }
}
